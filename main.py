import os
import sys
import datetime

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
            2 IDF
            3 TF*IDF
            4 BM25
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

# Run Lucene
def lucene():
    path_name = './trec-demo-master'
    sys.stdout.write("-------------------------------")
    sys.stdout.write("Starting script..")
    os.chdir(('{}').format(path_name))
    os.system('ant')
    os.system('ant IndexTrec')
    os.system('ant BatchSearch')

lucene()

# Run Ranklib
def ranklib():
    
    path_name = '/RankLib'
    os.system(('cd {}').format(path_name))
    ranking_models = ["MART", "RankNet", "RankBoost", "AdaRank", "Coordinate_Ascent",
                "", "LambdaMART", "ListNet", "Random_Forests", "L2_Regularization"]
    train_model = [6, 0]
    test_model = [6, 0, 8, 3]
    metric_train = "NDCG@10"
    metric_test = metric_train
    silent = "-silent"
    # General
    epoch = 300
    #lambdaMART and MART
    tree_size = 100
    tc = 256
    # random_forrest
    bag_size = 300
    r_tree = 1

    #Train the models in the models list
    def train_models(models):
        for x in models:
            start_time = datetime.datetime.now()
            sys.stdout.write(
                ("\n\nStarting training with {}\n").format(ranking_models[x]))
            if x == 0 or x == 6:
                save_model = (
                    "{}/{}_{}_model.txt").format(path_name,ranking_models[x], metric_train )
                os.system(("java -Xmx5500m -jar RankLib-2.1-patched.jar  -train {}/data/train.txt -ranker {} -validate {}/data/vali.txt -metric2t {} -tc {} -round {} -epoch {} -tree {} -save {} {}").format(
                    path_name, x, path_name, metric_train, tc, epoch, epoch, tree_size, save_model, silent))
                
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
                    "{}/{}_{}_model.txt").format(path_name,ranking_models[x], metric_train)
                os.system(("java -Xmx5500m -jar RankLib-2.1-patched.jar  -train {}/data/train.txt -ranker {} -validate {}/data/vali.txt -metric2t {} -tc {} -round {} -epoch {} -bag {} -tree {} -save {} {}").format(
                    path_name, x, path_name, metric_train, tc, epoch, epoch, bag_size, r_tree, save_model, silent))
            time = datetime.datetime.now()-start_time
            sys.stdout.write(str(time))
            sys.stdout.write(("\n...Finished {}\n").format(ranking_models[x]))
        sys.stdout.write("\n\nAll models are trained!\n\n\n")

    # Test the models in test list
    def rank_models(models):
        for x in models:
            sys.stdout.write(("Start ranking: {}\n").format(ranking_models[x]))
            #model, metric_test, ranker, test, metric, save
            write_results = (
                "{}/results/{}_{}_result.txt").format(path_name,ranking_models[x], metric_train)
            os.system(("java -Xmx5500m -jar RankLib-2.1-patched.jar -load {}/models/model_{}_{}.txt -ranker {} -test {}/data/test.txt -metric2T {} -tc 10  > {}").format(
                path_name,ranking_models[x], metric_test, x, path_name, metric_test, write_results))
        sys.stdout.write("\nFinished all the test models!\n\n")




