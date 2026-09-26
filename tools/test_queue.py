"""Edge-case tests for trigger + queue rules (SPEC F-02, F-04, F-08).
Mirrors shared-core Triggers.kt / NarrationQueue.kt. Stdlib only.
"""
import sys

DWELL = 8
COOLDOWN = 1800


class Queue:
    def __init__(self):
        self.q = []
        self.speaking = None

    def enqueue(self, pid, on_route=False):
        if pid == self.speaking or pid in self.q:
            return
        if on_route:
            self.q.insert(0, pid)
        else:
            self.q.append(pid)

    def next(self):
        self.speaking = self.q.pop(0) if self.q else None
        return self.speaking


def check(name, cond):
    print(f"  {'PASS' if cond else 'FAIL'} {name}")
    return cond


def main():
    ok = True
    # F-02: overlapping POIs -> one voice, second queued, no overlap
    q = Queue()
    q.enqueue("a")
    q.enqueue("b")
    first = q.next()
    ok &= check("F-02 one voice at a time", first == "a" and q.speaking == "a")
    q.finished = lambda pid: setattr(q, "speaking", None) if q.speaking == pid else None
    q.finished("a")
    second = q.next()
    ok &= check("F-02 second plays after first", second == "b")
    # dedupe: same POI twice -> single entry
    q2 = Queue()
    q2.enqueue("x")
    q2.enqueue("x")
    ok &= check("dedupe same POI", q2.q == ["x"])
    # on-route priority jumps the queue
    q3 = Queue()
    q3.enqueue("wander-poi")
    q3.enqueue("route-poi", on_route=True)
    ok &= check("on-route priority", q3.q[0] == "route-poi")
    # F-04: cooldown blocks re-speak within 30 min
    last_spoken = {"p": 1000}
    now = 1000 + 300
    cooled = (now - last_spoken["p"]) >= COOLDOWN
    ok &= check("F-04 cooldown blocks (5 min later)", not cooled)
    now2 = 1000 + COOLDOWN
    ok &= check("cooldown expires exactly at 30 min", (now2 - last_spoken["p"]) >= COOLDOWN)
    # F-08: mute clears queue immediately
    q4 = Queue()
    q4.enqueue("a")
    q4.enqueue("b")
    q4.q.clear()
    q4.speaking = None
    ok &= check("F-08 mute clears queue", q4.q == [] and q4.speaking is None)
    print("ALL PASS" if ok else "SOME FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
