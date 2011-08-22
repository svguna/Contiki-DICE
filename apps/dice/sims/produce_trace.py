#produce_single_trace.py node_distance num_nodes_swap distance_swap 
import os, glob, sys, math, random
from math import sqrt


def produce_trace(filename, node_distance, num_swapping_couples, distance_of_shuffles):
    x = []
    y = []
    dest_x = []
    dest_y = []
    node_num = 25
    experiment_duration = 3600
    static_time = 120
    distance_covered = 500
    node_direction = 1
    time = 0

    out = open(filename, "w")
    out.write("# FAKE-COW TRACES GENERATOR 1.0 by Marco Cattani\n")
#init nodes positions
    for i in range(0,int(sqrt(node_num))):
        for j in range(0,int(sqrt(node_num))):
            rx = i*(node_distance)
            ry = j*(node_distance)
            x.append(rx)
            y.append(ry)
            dest_x.append(rx)
            dest_y.append(ry)
    for t in range(0,experiment_duration+(distance_of_shuffles+(node_distance*4))):
        #move nodes
        for node in range(0,node_num):
            if x[node] < dest_x[node]:
                x[node] = x[node]+1
            if y[node] < dest_y[node]:
                y[node] = y[node]+1
            if x[node] > dest_x[node]:
                x[node] = x[node]-1
            if y[node] > dest_y[node]:
                y[node] = y[node]-1
            if (t%10==0):
                out.write(str(node)+' '+'%.2f'%(t)+' '+'%.6f'%(x[node])+' '+'%.6f'%(y[node]) + "\n")
        if (t%(distance_of_shuffles+(node_distance*4))) == 0 and t>0:
            #move nodes
            for node in range(0,node_num):
                #x[node] = dest_x[node]
                #y[node] = dest_y[node]
                dest_x[node] = x[node] + node_direction*distance_of_shuffles
            for i in range(0,num_swapping_couples):
                #select 2 random nodes
                random_node1 = int((random.random()*1000)%node_num)
                random_node2 = int((random.random()*1000)%node_num)
                #swap destination of 2 nodes
                tempx = dest_x[random_node1]
                tempy = dest_y[random_node1]
                dest_x[random_node1] = dest_x[random_node2]
                dest_y[random_node1] = dest_y[random_node2]
                dest_x[random_node2] = tempx
                dest_y[random_node2] = tempy
    out.close()
