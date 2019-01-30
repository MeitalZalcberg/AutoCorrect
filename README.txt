README file for auto correction program.
This program is here to help you find the right corrections to your typos.
Created by: Meital Zalcberg

Mode of use:
==========================================================================
as program arguments you have to give in the paths for 4 confusion matrix's,
and the path to the "corpus". The corpus is a mass in the language we are
working with and it should be as big as possible so that the probabilities
for each word would be resonable. If no corpus is available, we can give
as input a path to a file containing the list of the words that are the 
"dictionary". The distribution between the words would be uniform.
args should look like this:
path to transition matrix, path to substitution matrix, path to insert matrix, 
path to del matrix, path to corpus. 

after running the program with the right parameters, a message would be shown
asking you to input a string to the system, after which it will return the 
candidates and their scores. 

The algorithm:
==========================================================================
I implemented a two steps algorythm. The first step of the algorithm
is based on the Demaru levenshtein distance, ain which I gathered all 
words that are 1 or 2 edit distance from the typed word.
To speed up running time, I used dynamic programming. (This solution uses
more space but since most words are'nt too long I decided that was a better
implementation than the recursive one).
The second stage of the algorithm is scoring the candidates. 
The scores were calculated the following way:
score(candidate|typo) = probability(word) * probability(change)
after that, scores were normalized.
let's get into explanations:
probability(word) = as discussed above, that would be the frequency
in which the word appeard.
Then, probability(change) would be:
Assuming we only have one change of the following:
Insertion, deletion, substitution, transposition. 
t = type, c = candidate
and given p is the letter in which the error occured, 

			|del[c(p-l), c(p)]/chars[c(p-l), c(p)] if deletion
Pr(t|c)  =  |add[c(p-1), t(p)]/chars[ c(p-1)] if insertion
			|sub[t(p), c(p)]/chars[c(p)] if substitution
			|rev[c(p), C(p+1)]/chars[c(p), c(p+1)] if transposition.

Where del, add, sub, rev are the confusion matrixs respectively. 			
Do note that the confusion matrix's are based on real life errors, 
and therefore take in consideration phonetics and keyboard proximity. 

Discussion and further work:
==========================================================================
The algorithm assumes a large dictionarie. I didn't have time to check and 
acore two mistakes and more. given more time i would have a better probability 
functions for two, three and even four mistakes. 
until then - please try the auto correct with the 'words' corpus that contains more words. 

The algorithm gives a bit too much weight to the ptrs feature.
depending on the corpus, i would change the probability score 
to show more relevant candidates. In addotion, a field specific corpus
or personalized corpus would make the program even more efficient as well.

Given more time, it would be good to have a method saving process, for 
example probability counts and not counting it all over again each time. 

Tests tests and more tests. 



Expantion and changes:
==========================================================================
The basics are here - given other languges, it would be needed to have relevant confusion
matrixs, accoding to the languages form. 
In addition, the score can be changed to be more suitable for other uses or languages. 

Libraries:
==========================================================================
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Stream;


Referenses:
==========================================================================
1. Demaru levenshtein explained:
https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance
2. The second stage of the algorithm is inspired by Kernighan et al. article - 
A spelling correction based on noisy Channel model
http://www.aclweb.org/anthology/C90-2036



