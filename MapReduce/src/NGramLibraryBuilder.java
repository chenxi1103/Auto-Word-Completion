import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class NGramLibraryBuilder {
	public static class NGramMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

		int numGram;
		@Override
		public void setup(Context context) {
		    // Set the value of N here. (N-Gram's N should not be constant since the value might be changed based on requirement)
		    // Read this value from commend line transported by conf1 in Driver
		    Configuration conf1 = context.getConfiguration();
				// Set the default value to be 5 (sometimes user may forget to input the value of N!)
		    numGram = conf1.getInt("numGram", 5);
		}
		/*
		 *  Mapper Method
		 *  The input variable "key" record the NO. of the sentence that located in the document, which is useless here (won't use it).
		 *  The input variable "value" represents current sentence read from the documents.
		 *  PS: The default method to read the document is read "line by line (\n)", need to change this to be
		 *  read "sentence by sentence" - Change the "textinputformat.record.delimiter" to be "[.?!;:]$" in Driver
		 */
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			// Line is the current sentence read from the document
			String line = value.toString();
			// Remember to trim the line and convert to lower case since this is not case sensitive, and remember to remove all non-alphabetic symbols
			line = line.trim().toLowerCase().replace("[a-z]", " ");


			// Build N-Gram model
			String[] words = line.split(" ");
			// First determine this sentense only has one word (Ingore it! 1-Gram is useless! But give a error log to show this exception.
			if(words.length < 1) {
			    System.err.println("This sentence does not have anything! So we drop it off");
			    return;
			}
			if(words.length == 1) {
			    System.err.println("This sentence only have one word: "+ words[0]+" So we drop it off");
			    return;
			}

			// Start to build N-Gram model
			for(int i = 0; i<words.length-1; i++) {
			    StringBuffer nPhrase = new StringBuffer();
			    nPhrase.append(words[i]);
			    // Check if the phrase is larger than number of gram
			    for(int j = i+1; j< words.length && j-i < numGram; j++) {
			        nPhrase.append(" ");
			        nPhrase.append(words[j]);
			        //Write the key-value pair (key: current phrase, value: 1) into context
			        context.write(new Text(nPhrase.toString()), new IntWritable(1));
			    }
			}
		}
	}


	public static class NGramReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		/*
		 * Reducer Method. Because of the shuffle process, same key phrases are collected together
		 * So all the key would be the same phrase, al the values would be 1
		 * Only need to sum them together and write into context as key-value pair
		 */
		@Override
		public void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
					int sum = 0;
					for(IntWritable value : values){
						sum += value;
					}
					context.write(key, new IntWritable(sum));
		}
	}
}
