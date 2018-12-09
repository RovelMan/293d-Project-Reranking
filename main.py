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
    ---------------------------------------------------------------

    LETOR - Learning To Rank

        <line> .=. <target> qid:<qid> <feature>:<value> <feature>:<value> ... <feature>:<value> # <info>
        
        Generate the correct file format to be used in ranklib
        Features:
            1 TF
                whole
                text
                headline
                cn
            2 IDF
            whole
                text
                headline
                cn
            3 TFIDF
            whole
                text
                headline
                cn
            4 BM25
            whole
                text
                headline
                cn
            5  LM
            whole
                text
                headline
                cn
            6 DMR
            whole
                text
                headline
                cn
            5 DL - document length


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
predict = False
model_train = True
lucene_train = False

def write_chunk(lines):
    data_size = len(lines)
    random.shuffle(lines)
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
        os.system(('java -cp "bin:lib/*" BatchSearch -index index/ -queries test-data/test-queries.txt -top 10 -train {} -simfn bm25').format(train))
    if train:
        os.system('ant')
        # os.system('ant IndexTREC')
        os.system(('java -cp "bin:lib/*" BatchSearch -index index/ -queries test-data/title-queries.301-450 -top {} -train {} -simfn bm25').format(top_docs,train))
        sys.stdout.write('  Generating data\n\n')
        f = open('../RankLib/data/letor.txt','r')
        lines = f.readlines()
        f.close()
        write_chunk(lines)
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
    test_model = [6, 0, 3]
    pred_model = [3]
    metric_train = "NDCG@10"
    metric_test = metric_train
    silent = "-silent"

    # General
    epoch = 10000
    k_fold = 3
    gmax = 2
    #lambdaMART and MART
    tree_size = 1000
    tc = 256
    # Adarank uses default param



    # random_forrest
    bag_size = 1000
    r_tree = 1

    #Train the models in the models list
    def train_models(models):
        for x in models:
            start_time = datetime.datetime.now()
            sys.stdout.write(
                ("\n\nStarting training with {}\n").format(ranking_models[x]))
            if x == 0 or x == 6:
                save_model = (
                    "models/{}_{}_model.txt").format(ranking_models[x], metric_train )
                os.system(("java -jar RankLib-2.1-patched.jar  -train data/train.txt -validate data/vali.txt -ranker {} -kcv {} -metric2t {} -tc {} -round {} -kcv 3  -gmax {} -epoch {} -tree {} -save {} {}").format(
                    x, k_fold,metric_train, tc, epoch, gmax,epoch, tree_size, save_model, silent))
                
                """Preprocess to draw heatmap"""
                # f = open(
                #     ("./MQ2008/models/model_{}_{}_f{}.xml").format(ranking_models[x], metric_train, ), "r+")
                # lines = f.readlines()
                # f.seek(0)
                # for i in lines:
                #     if "#" not in i:
                #         f.write(i)
                # f.truncate()
                # f.close()
                # sys.stdout.write("\nDraw heatmap..\n")
                # os.system(("python ./draw_tree.py ./MQ2008/models/model_{}_{}_f{}.xml | dot -Tpng > ./MQ2008/models/model_heatmap_{}_{}_f{}.png").format(
                #     ranking_models[x], metric_train,, ranking_models[x], metric_train,))
            else:
                # format: train, ranker, test, validate, metric, metric
                save_model = (
                    "models/{}_{}_model.txt").format(ranking_models[x], metric_train)
                os.system(("java -jar RankLib-2.1-patched.jar  -train data/train.txt -ranker {} -validate data/vali.txt -metric2t {} -tc {} -round {} -gmax {} -epoch {} -bag {} -tree {} -kcv {} -save {} {}").format(
                    x, metric_train, tc, epoch, gmax, epoch, bag_size, r_tree, k_fold, save_model, silent))
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
                os.system(("java -jar RankLib-2.1-patched.jar -load models/{}_{}_model.txt -ranker {} -test data/test.txt -metric2T {} -tc 10  > {}").format(
                    ranking_models[x], metric_test, x, metric_test, write_results))
            if(pred):
                os.system(("java -jar RankLib-2.1-patched.jar -load models/{}_{}_model.txt -rank data/predict.txt -score results/rerank_scores/{}_{}_scores.txt").format(
                ranking_models[x], metric_test,ranking_models[x],metric_test))
                f = open(('results/rerank_scores/{}_{}_scores.txt').format(ranking_models[x],metric_test))
                lines = f.readlines()
                f.close()
                top_10 = []
                for line in lines:
                    rank = float(line.split("\t")[2].strip("\n"))
                    top_10.append(rank)
                top_10_sorted = top_10.sort(reverse=True)
                print(top_10)
                print(top_10_sorted)

        sys.stdout.write("\nFinished all the test models!\n\n")
    if(train):
        train_models(train_model)
        rank_models(train_model,False)
    if(predict):
        rank_models(pred_model, True)

lucene(topdocs,lucene_train)
ranklib(train=model_train,pred=predict)



