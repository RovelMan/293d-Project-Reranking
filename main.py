import os
import sys
import datetime
import mmap
import random

"""
    ---------------------------------------------------------------
    Set terminal path to './project'

    To write to cmd
        sys.stdout.write()

    To execute command lines
        os.system()

    ---------------------------------------------------------------

    Lucene
    
        Find all features

        Return top n documents

    ----------------------------------------------------------------
    RankLib

    Specify which ranking algorithm to use    
            0: MART (gradient boosted regression tree)
            1: RankNet
            2: RankBoost
            3: AdaRank
            4: Coordinate Ascent
            6: LambdaMART
            7: ListNet
            8: Random Forests
            9: Linear regression (L2 regularization) 

    ----------------------------------------------------------------

    Choose desired metric
            MAP, NDCG@k, DCG@k, P@k, RR@k, ERR@k

            metric_train to optimize on the training data.
            metric_test to evaluate on the test data (default=ERR@10)

    -----------------------------------------------------------------

"""

topdocs = 250
model_train = True
feat_extract = False
predict = False

arguments = sys.argv[1:]
for i in range(0,len(arguments)-1,2):
    if arguments[i] == "--feat_extract":
        print(bool(arguments[i+1]))
        feat_extract = bool(arguments[i+1])
    if arguments[i] == "--train_models":
        model_train = bool(arguments[i+1])
    if arguments[i] == "--predict":
        predict = bool(arguments[i+1])


def write_chunk(lines):
    data_size = len(lines)
    # random.shuffle(lines)
    train = lines[int(data_size*0):int(data_size*0.70)]
    vali = lines[int(data_size*0.70):int(data_size*0.85)]
    test = lines[int(data_size*0.85):int(data_size*1)]

    train_file = open('../RankLib/data/train.txt','w')
    vali_file = open('../RankLib/data/vali.txt','w')
    test_file = open('../RankLib/data/test.txt','w')
    for line in train:
        train_file.write(line)
    for line in vali:
        vali_file.write(line)
    for line in test:
        test_file.write(line)
    train_file.close()
    vali_file.close()
    test_file.close()


# Run Lucene
def lucene(top_docs,train):
    path_name = './trec-demo-master'
    sys.stdout.write("-------------------------------\n\n")
    sys.stdout.write("  Starting Lucene()\n\n")
    os.chdir(('{}').format(path_name))
    sys.stdout.write("  Running lucene\n\n")
    if predict:
        os.system(('java -cp "bin:lib/*" BatchSearch -index index/ -queries test-data/prediction-queries.txt -top 10 -train {} -simfn bm25').format(train))
    if train:
        os.system('ant')
        os.system('ant IndexTREC')
        os.system(('java -cp "bin:lib/*" BatchSearch -index index/ -queries test-data/title-queries.301-450 -top {} -train {} -simfn bm25').format(top_docs,train))
        sys.stdout.write('  Generating data\n\n')

        # IF YOU DONT WANT TO USE K-FOLD CROSS VALIDATION, UNCOMMENT THIS AND IT WILL SPLIT THE LETOR FILE
        # f = open('../RankLib/data/letor.txt','r')
        # lines = f.readlines()
        # f.close()
        # data_size = len(lines)
        # # write_chunk(lines)
        # random.shuffle(lines)
        # train = lines[int(data_size*0):int(data_size*0.85)]
        # test = lines[int(data_size*0.85):int(data_size*1)]
        # f_train = open('../RankLib/data/train.txt','w')
        # f_test = open('../RankLib/data/train.txt','w')
        # for line in train:
        #     f_train.write(line)
        # for line in test:
        #     f_test.write(line)
    sys.stdout.write('  Done with Lucene()\n\n')
    sys.stdout.write('-------------------------------\n\n')
    os.chdir('..')


# Run Ranklib
def ranklib(train=False, pred=False):
    sys.stdout.write('\n\n-------------------------------\n\n')
    sys.stdout.write('  Starting RankLib()\n\n')
    path_name = './RankLib'

    os.chdir(('{}').format(path_name))
    ranking_models = ["MART", "RankNet", "RankBoost", "AdaRank", "Coordinate_Ascent",
                "", "LambdaMART", "ListNet", "Random_Forests", "L2_Regularization"]
    train_model = [6, 0, 3]
    test_model = [6, 0, 3, 1]
    pred_model = [6,0,3]
    metric_train = "NDCG@10"
    metric_test = metric_train
    silent = "-silent"

    # General
    k_fold = 3
    gmax = 4
    #lambdaMART and MART
    tree_size = 1000
    tc = 256
    # Adarank
    rounds = 300
    #ranknet
    epoch = 100
    layers = 3
    nodes = 20

    # random_forrest
    r_tree = 1

    #Train the models in the models list
    def train_models(models):
        for x in models:
            start_time = datetime.datetime.now()
            sys.stdout.write(
                ("\n\nStarting training with {}\n").format(ranking_models[x]))
            if x == 0 or x == 6:
                print("train-exe tree")
                save_model = (
                    "models/{}_{}_model.txt").format(ranking_models[x], metric_train )
                save_name = ("{}_{}_model.txt").format(ranking_models[x], metric_train)
                os.system(("java -jar RankLib-2.1-patched.jar  -train data/letor.txt -ranker {} -kcv {} -metric2t {} -tc {} -round {}  -gmax {} -epoch {} -tree {} -kcvmd models/ -kcvmn {} {}").format(
                    x, k_fold,metric_train, tc, epoch, gmax,epoch, tree_size, save_name, silent))
                
                """Preprocess to draw heatmap"""
                f = open(
                    ("models/f3.{}_{}_model.txt").format(ranking_models[x], metric_train, ), "r+")
                lines = f.readlines()
                f_xml = open(("models/{}_{}_model.xml").format(ranking_models[x],metric_test),"w")
                f.seek(0)
                for i in lines:
                    if "#" not in i:
                        f_xml.write(i)
                f.truncate()
                f.close()
                f_xml.close()
                sys.stdout.write("\nDraw heatmap..\n")
                os.system(("python ./draw_tree.py models/{}_{}_model.xml | dot -Tpng > models/model_heatmap_{}_{}.png").format(
                    ranking_models[x], metric_train, ranking_models[x], metric_train,))
            elif x == 1:
                save_model = (
                    "models/{}_{}_model.txt").format(ranking_models[x], metric_train)
                save_name = ("{}_{}_model.txt").format(ranking_models[x], metric_train)
                os.system(("java -jar RankLib-2.1-patched.jar  -train data/letor.txt -ranker {} -metric2t {} -tc {} -round {} -gmax {} -layer {} -node {} -epoch {} -kcv {} -kcvmd models/ -kcvmn {} {}").format(
                    x, metric_train, tc, epoch, gmax, layers, nodes, epoch,  k_fold, save_name, silent))
            else:
                # format: train, ranker, test, validate, metric, metric
                save_model = (
                    "models/{}_{}_model.txt").format(ranking_models[x], metric_train)
                save_name = ("{}_{}_model.txt").format(ranking_models[x], metric_train)
                os.system(("java -jar RankLib-2.1-patched.jar  -train data/letor.txt -ranker {} -metric2t {} -tc {} -round {} -gmax {}  -epoch {} -tree {} -kcv {} -kcvmd models/ -kcvmn {} {}").format(
                    x, metric_train, tc, rounds, gmax, epoch, r_tree, k_fold, save_name, silent))
            time = datetime.datetime.now()-start_time
            sys.stdout.write(str(time))
            sys.stdout.write(("\n...Finished {}\n").format(ranking_models[x]))
        sys.stdout.write("\n\nAll models are trained!\n\n\n")

    # Test the models in test list
    def rank_models(models, pred):
        for x in models:
            sys.stdout.write(("Start ranking: {}\n").format(ranking_models[x]))
            #model, metric_test, ranker, test, metric, save
            write_results = (
                "results/{}_{}_result.txt").format(ranking_models[x], metric_train)
            if(not pred): 
                os.system(("java -jar RankLib-2.1-patched.jar -load models/f3.{}_{}_model.txt -ranker {} -test data/letor.txt -metric2T {} -tc 10  > {}").format(
                    ranking_models[x], metric_test, x, metric_test, write_results))
            if(pred):
                os.system(("java -jar RankLib-2.1-patched.jar -load models/f3.{}_{}_model.txt -rank data/predict.txt -score results/rerank_scores/{}_{}_scores.txt").format(
                ranking_models[x], metric_test,ranking_models[x],metric_test))
                f = open('data/predict.txt','r')
                lines = f.readlines()
                f.close()
                docno = []
                for line in lines:
                    docno.append(line.split(' ')[-1])

                f = open(('results/rerank_scores/{}_{}_scores.txt').format(ranking_models[x],metric_test),'r')
                lines = f.readlines()
                f.close()
                counter = 0
                new_lines = []
                for line in lines:
                    line = line.strip('\n').split("\t")
                    line.append(docno[counter].strip("\n"))
                    new_lines.append(line)
                    counter+=1
                new_lines = sorted(new_lines,key=lambda line:float(line[2]),reverse=True)
                f = open(('results/rerank_scores/{}_{}_scores.txt').format(ranking_models[x],metric_test),'w')
                for line in new_lines:
                    line_to_write=' '.join(line)+'\n'
                    f.write(line_to_write)

        sys.stdout.write("\nFinished all the test models!\n\n")
    if(train):
        train_models(train_model)
        rank_models(train_model,False)
        
    if(predict):
        rank_models(pred_model, True)


lucene(topdocs,feat_extract)
ranklib(train=model_train,pred=predict)



