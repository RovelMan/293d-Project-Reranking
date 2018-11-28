from keras.models import Model
from keras.layers import Input, Dense, Reshape, merge
from keras.layers.embeddings import Embedding
from keras.preprocessing.sequence import skipgrams
from keras.preprocessing import sequence

import urllib
import collections
import os
import zipfile

import numpy as np
import tensorflow as tf

def build_dataset(words, n_words):
    """
        Process raw inputs into a dataset.
        Basically what it does is creating a dataset
        with n_words, setting the frequency for each
        word as the data.
    
    """
    count = [['UNK', -1]]
    #count word frequency for top n_words into a list
    count.extend(collections.Counter(words).most_common(n_words - 1))
    dictionary = dict()
    #create a dictinoary from the list
    for word, _ in count:
        dictionary[word] = len(dictionary)
    data = list()
    unk_count = 0
    for word in words:
        if word in dictionary:
            index = dictionary[word]
        else:
            index = 0  # dictionary['UNK']
            unk_count += 1
        # print("found word", word, index, unk_count)
        data.append(index)
    count[0][1] = unk_count
    reversed_dictionary = dict(zip(dictionary.values(), dictionary.keys()))
    return data, count, dictionary, reversed_dictionary

def collect_data(vocabulary_size=10000):
    f = open(path)
    lines = f.readlines()
    f.close()
    vocabulary = lines[0].split(" ")
    data, count, dictionary, reverse_dictionary = build_dataset(vocabulary,vocabulary_size)
    del vocabulary  # Hint to reduce memory.
    return data, count, dictionary, reverse_dictionary

path = './text8.txt'
vocab_size = 10
generate_data = True
if generate_data:
    print(('\nGenerating data from {}\n').format(path))
    data, count, dictionary, reverse_dictionary = collect_data(vocabulary_size=vocab_size)
    print(("Lengths: {}, {}, {}, {}").format(len(data),len(count),len(dictionary),len(reverse_dictionary)))
    print('\nDone generating data\n')
    f = open('generated_data.txt','w')
    index = 0
    for d in data:
        f.write(str(d))
        if index < 10:
            f.write(('\t{}\t{}\t{}').format(count[index],dictionary[index],reverse_dictionary[index]))
        index+=1
        f.write('\n')
    # dictionary,reverse_dictionary)
    f.close()
else:
    f.open('generated_data.txt','r')
    f.readlines()
    

window_size = 3
vector_dim = 300
epochs = 10
data_size = len(data)

valid_size = 16     # Random set of words to evaluate similarity on.
valid_window = 100  # Only pick dev samples in the head of the distribution.
valid_examples = np.random.choice(valid_window, valid_size, replace=False)
print(('\nValid examples: {}\n').format(valid_examples))

sampling_table = sequence.make_sampling_table(vocab_size)
print(("Sampling table: \n{}").format(sampling_table[:10]))
couples, labels = skipgrams(data, vocab_size, window_size=window_size, sampling_table=sampling_table)
print(("\nCouples:\n{}\nLabels:\n{}").format(couples[:10],labels[:10]))









