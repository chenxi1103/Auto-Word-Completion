# Automatically-Word-Completion
Realized Auto-Word-Completion by :
- Algorithm: N-Gram model
- Backend: MapReduce/MySQL
- Frontend: PHP/Ajax/JQuery

### Algorithm: N-Gram Model
- N-Gram model is a kind of popular NLP algorithm.
> Gram represents “letter”
A "N-Gram” is a CONTIGUOUS sequence of n items from a given sequence of text of speech.

- Predict N Gram based on N Gram: 
> First use MapReduce realize predict 1 Gram based on N Gram
Then put all the data into MySQL to realize predict N Gram based on N Gram.

## Realizing Steps General:
1. Read large-scale document collections
2. Build N-Gram Library
3. Calculate probability
4. Run project on MapReduce / Implement with MySQL, store the structured data into MySQL
5. Build visualization by Ajax/JQuery/PHP implementing with MySQL to show the autocomplete search result.

### 1. Read Large-scale document collections
- Read document collections from HDFS
- Read each document “sentence by sentence”, not “line by line”, because they have to be semantic!
- Need to remove all non-alphabetic symbols

### 2. Build N-Gram Library
- Did by first MapReduce Job
- 1-Gram is useless, needs to be skipped.
- Calculate the total count from 2-Gram to N-Gram
> For example: “I love big data.” In this case, we need to calculate 2-Gram to 4-Gram(since this sentence has 4 words in total)
             1-Gram: I / love / big / data (useless, cannot provide any association)
             2-Gram: I love / love big/ big data
             3-Gram: I love big/ love big data
             4-Gram: I love big data
- Write the output into HDFS

### 3. Build Language Model
- Did by second MapReduce Job
- Read the output from the first job (from HDFS)
- Mapper Task: Split the input phrase into two part, first part is the new key (possible input from user), second part is the possible following phrase, and we have the value of the frequency. Since we only want to realize predicting 1 Gram based on N Gram in MapReduce (and realize predicing N Gram based on N Gram in Database), so the second part (possible following phrase) of splited phrase should be the last word of the original phrase and the previous words should be the possible user input. The output of Mapper should also be key-value pair. Key is the first part ot the splited phrase. Value should be second part of the splited phrase + count. For example: Key: I love big / Value: data = 100. 
- Optimization in mapper: Since transporting data cost a lot, we may set a threshold to filter some data in the mapper. For example, if the count of a phrase is below 100, it is unnecessary to transport it to reducer to do topK since we are confident that it is too small to be selected into topK, so we can simply ignore it.
- Reducer Task: After the shuffle process, the possible values with the same key were collected together. The reduce should first extract the count for each value (do .split(“=“)) and sort them. Only persist the top K values to write them into Database(MySQL) (Save the memory! And we don’t need unimportant ones.) by implementing DBWritable Class.
### 4. Realize predicting N Gram based on N Gram in MySQL
- Use fuzzy query to realize predict N based on N
> For example, if we do exact query for “I love", we only get the following word that exactly followed by “I love”. But if we do fuzzy query, we can get the following word that not only start with “I love”, but also start with something like “I love”, like “I love big”, and “I love great”, which realized predicting N by N in a subtle way.
- MySQL Query Code
> select * from output where starting_phrase like “input%”
order by count
limit 10;

### Extension Problems:
- Why Database(MySQL) is required:
> Autocomplete task requires a key-value pair structure to realize. Everytime when user input a word or phrase, the frontend would sent the data(this word/phrase) to backend to implement with database to do query job and get the result to display on the website.
Input phrase/words is key, associate phrase/words is value

- Why not HashMap:
> HashMap could not store very large dataset
And if a blackout happen, the data would be lost.

- What Data should be stored in the MySQL:
> First column: Input phrase
Second column: following phrase
Third column: Probability
