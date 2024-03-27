#!/usr/bin/env python3

import zstandard
import sys

for fname in sys.argv[1:]:
    print(f"{fname} = ", end="")
    try:
        with open(fname, "rb") as f:
            data = f.read()
        p = zstandard.get_frame_parameters(data)
        print(("FrameParameters{"
               f"content_size={p.content_size}(0x{p.content_size:x})"
               f",window_size={p.window_size}(0x{p.window_size:x})"
               f",dict_id={p.dict_id}"
               f",has_checksum={p.has_checksum}"
               "}"))
    except Exception as e:
        print(f"ERROR: {e}")
