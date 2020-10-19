import os
import csv

algos = ['pullBackIncreasingRadi', 'pullBackDecreasingRadi', 'pullBackCentralFirst', 'pullBackGreedyDirections',
         'centerAreaSpread', 'pushAlgorithm', 'lpPush', 'lpCenterPush']
countbestAlgo = {algo:0 for algo in algos}
avgtimeAlgo = {algo:0 for algo in algos}
countinvalidAlgo = {algo:0 for algo in algos}
toppercentileAlgo = {algo:0 for algo in algos}

i = 0
for filename in os.listdir('results'):
    i += 1
    reader = csv.reader(open('results/' + filename, 'r'))
    bestalgo, bestvalid, bestquality = None, False, None

    for row in reader:
        problem, algo, time, valid, quality = row
        time = float(time)
        valid = valid == 'true'
        quality = float(quality)
        avgtimeAlgo[algo] += time
        if not valid:
            countinvalidAlgo[algo] += 1
        if (valid and not bestvalid) or (valid and quality < bestquality):
            bestproblem, bestalgo, besttime, bestvalid, bestquality = problem, algo, time, valid, quality
    countbestAlgo[bestalgo] += 1

    reader = csv.reader(open('results/' + filename, 'r'))
    for row in reader:
        problem, algo, time, valid, quality = row
        valid = valid == 'true'
        quality = float(quality)
        if valid and quality < 1.05 * bestquality:
            toppercentileAlgo[algo] += 1

print('total number of problems =', i)
for algo in algos:
    avgtimeAlgo[algo] = avgtimeAlgo[algo] / i
    print(algo, 'countbest =', countbestAlgo[algo], ', avgtime =', avgtimeAlgo[algo], ', timesinvalid =', countinvalidAlgo[algo], ', counttop5% =', toppercentileAlgo[algo])
