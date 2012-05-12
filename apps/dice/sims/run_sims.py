from os import mkdir, system
from produce_trace import produce_trace
from re import sub
from threading import Thread, Semaphore

node_distances = [8, 15, 30]
num_swaps = [0, 5, 10]
dist_swaps = [10, 50, 100]
repetitions = range(0, 64)
threads = 2

try:
    mkdir('positions')
except OSError:
    pass
try:
    mkdir('results')
except OSError:
    pass
try:
    mkdir('csc')
except OSError:
    pass


def run_repetition(nd, ns, ds, r):
    print "Running node dist", nd, "num swaps", ns, "dist swaps", ds, 
    print "repeat", r
    id = str(nd) + "_" + str(ns) + "_" + str(ds) + "_" + str(r)
    positions = "pos_" + id + ".txt"
    cscname = "sim_" + id + ".csc"
    produce_trace("positions/" + positions, nd, ns, ds)
    csc = open("csc/" + cscname, "w")
    for line in open("sim.csc.template"):
        csc.write(sub("xxPOSITION_FILExx", positions, line))
    csc.close()
    system("java -Xmx256m -Doutputfile=results/log" + id + ".txt -jar ../../../tools/cooja/dist/cooja.jar -nogui=csc/"+ cscname)
    system("bzip2 results/log" + id + ".txt")


class WrapperThread(Thread):
    def __init__(self, nd, ns, ds, r, finish_sem):
        Thread.__init__(self)
        self.nd = nd
        self.ns = ns
        self.ds = ds
        self.r = r
        self.finish_sem = finish_sem

    def run(self):
        run_repetition(self.nd, self.ns, self.ds, self.r)
        self.finish_sem.release()


for nd in node_distances:
    for ns in num_swaps:
        for ds in dist_swaps:
            for r in repetitions:
                finish_sem = Semaphore(0)
                for i in range(0, threads):
                    WrapperThread(nd, ns, ds, r * threads + i, finish_sem).start()
                for i in range(0, threads):
                    finish_sem.acquire()


