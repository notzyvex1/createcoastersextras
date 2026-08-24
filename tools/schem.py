"""Minimal Sponge Schematic v3 (.schem) reader/writer.

Written because decorating showcase.schem means rewriting the Palette and Data
of an existing file while leaving BlockEntities and everything else byte-exact.
A partial parser would silently drop tags it did not understand and produce a
file that looks fine until Minecraft refuses to load it, so this handles the
full NBT tag set and round-trips.
"""
import gzip, struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE = 0, 1, 2, 3, 4, 5, 6
BYTES, STRING, LIST, COMPOUND, INTS, LONGS = 7, 8, 9, 10, 11, 12


class Tag:
    """Keeps the tag id alongside the value; NBT is not self-describing on read-back."""
    __slots__ = ("id", "val")

    def __init__(self, id_, val):
        self.id, self.val = id_, val

    def __repr__(self):
        return f"Tag({self.id},{self.val!r})"


class Reader:
    def __init__(self, buf):
        self.b, self.i = buf, 0

    def u1(self):
        v = self.b[self.i]; self.i += 1; return v

    def raw(self, fmt, n):
        v = struct.unpack_from(fmt, self.b, self.i)[0]; self.i += n; return v

    def string(self):
        n = self.raw(">H", 2)
        v = self.b[self.i:self.i + n].decode("utf8"); self.i += n; return v

    def payload(self, t):
        if t == BYTE:    return self.raw(">b", 1)
        if t == SHORT:   return self.raw(">h", 2)
        if t == INT:     return self.raw(">i", 4)
        if t == LONG:    return self.raw(">q", 8)
        if t == FLOAT:   return self.raw(">f", 4)
        if t == DOUBLE:  return self.raw(">d", 8)
        if t == STRING:  return self.string()
        if t == BYTES:
            n = self.raw(">i", 4)
            v = self.b[self.i:self.i + n]; self.i += n; return v
        if t == INTS:
            n = self.raw(">i", 4)
            v = list(struct.unpack_from(f">{n}i", self.b, self.i)); self.i += 4 * n; return v
        if t == LONGS:
            n = self.raw(">i", 4)
            v = list(struct.unpack_from(f">{n}q", self.b, self.i)); self.i += 8 * n; return v
        if t == LIST:
            it = self.u1(); n = self.raw(">i", 4)
            return (it, [self.payload(it) for _ in range(n)])
        if t == COMPOUND:
            out = {}
            while True:
                ct = self.u1()
                if ct == END: return out
                # Name must be read into a local first. Written as
                #   out[self.string()] = Tag(ct, self.payload(ct))
                # Python evaluates the right-hand side before the subscript, so payload()
                # consumed the bytes holding the name and the parse silently derailed.
                key = self.string()
                out[key] = Tag(ct, self.payload(ct))
        raise ValueError(f"unknown tag {t}")


class Writer:
    def __init__(self):
        self.parts = []

    def w(self, d):
        self.parts.append(d)

    def string(self, s):
        e = s.encode("utf8"); self.w(struct.pack(">H", len(e))); self.w(e)

    def payload(self, t, v):
        if t == BYTE:    self.w(struct.pack(">b", v))
        elif t == SHORT: self.w(struct.pack(">h", v))
        elif t == INT:   self.w(struct.pack(">i", v))
        elif t == LONG:  self.w(struct.pack(">q", v))
        elif t == FLOAT: self.w(struct.pack(">f", v))
        elif t == DOUBLE:self.w(struct.pack(">d", v))
        elif t == STRING:self.string(v)
        elif t == BYTES: self.w(struct.pack(">i", len(v))); self.w(bytes(v))
        elif t == INTS:  self.w(struct.pack(">i", len(v))); self.w(struct.pack(f">{len(v)}i", *v))
        elif t == LONGS: self.w(struct.pack(">i", len(v))); self.w(struct.pack(f">{len(v)}q", *v))
        elif t == LIST:
            it, items = v
            self.w(bytes([it])); self.w(struct.pack(">i", len(items)))
            for x in items: self.payload(it, x)
        elif t == COMPOUND:
            for k, tag in v.items():
                self.w(bytes([tag.id])); self.string(k); self.payload(tag.id, tag.val)
            self.w(bytes([END]))
        else:
            raise ValueError(f"unknown tag {t}")


def load(path):
    r = Reader(gzip.open(path, "rb").read())
    t = r.u1(); name = r.string()
    return name, Tag(t, r.payload(t))


def save(path, name, root):
    w = Writer()
    w.w(bytes([root.id])); w.string(name); w.payload(root.id, root.val)
    with gzip.open(path, "wb") as f:
        f.write(b"".join(w.parts))


def varints(data):
    """Decode the Data byte array into palette indices."""
    out, j = [], 0
    while j < len(data):
        v, sh = 0, 0
        while True:
            b = data[j]; j += 1
            v |= (b & 0x7F) << sh
            if not (b & 0x80): break
            sh += 7
        out.append(v)
    return out


def to_varints(vals):
    out = bytearray()
    for v in vals:
        while True:
            b = v & 0x7F; v >>= 7
            out.append(b | (0x80 if v else 0))
            if not v: break
    return bytes(out)
