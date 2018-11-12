import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.db.DBConfiguration;
import org.apache.hadoop.mapreduce.lib.db.DBOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;


public class Driver {

	public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
		// Three variables for job1
		// Input directory (documents collections)
		String inputDir = args[0];
		// Output directory (HDFS)
		String nGramLib = args[1];
		String numNGram = args[2];

		//Two variables for job2
		String threshold = args[3];
		// Top K (k value)
		String numFollowingWords = args[4];

	  //job1
		Configuration conf1 = new Configuration();

		//Customize the separator to be ".?!;" (read "sentence" by "sentence") (default is "\n")
		conf1.set("textinputformat.record.delimiter","[.?!;:]$");

		//Set the num of gram by value from command line
		conf1.set("numGram", numNGram);

		Job job1 = Job.getInstance(conf1);
		job1.setJobName("NGram");
		// Have to compress the files to .jar and inform Hadoop where is the main method
		job1.setJarByClass(Driver.class);

		job1.setMapperClass(NGramLibraryBuilder.NGramMapper.class);
		job1.setReducerClass(NGramLibraryBuilder.NGramReducer.class);

		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);

		job1.setInputFormatClass(TextInputFormat.class);
		job1.setOutputFormatClass(TextOutputFormat.class);

		TextInputFormat.setInputPaths(job1, new Path(inputDir));
		TextOutputFormat.setOutputPath(job1, new Path(nGramLib));
		job1.waitForCompletion(true);

		//2nd job
		Configuration conf2 = new Configuration();
		conf2.set("threshold", threshold);
		conf2.set("n", numFollowingWords);

		DBConfiguration.configureDB(conf2,
				"com.mysql.jdbc.Driver",
				"jdbc:mysql://128.237.193.111:port/data1", //IP address and Database name
				"root", //username
				"root"); //password

		Job job2 = Job.getInstance(conf2);
		job2.setJobName("Model");
		job2.setJarByClass(Driver.class);

		// Add dependency to the class path
		// 1. upload the dependency to HDFS
		// 2. use "addArchiveToClassPath" method to define dependency path on HDFS
		job2.addArchiveToClassPath(new Path("/mysql/mysql-connector-java-5.1.39-bin.jar"));
		// If the mapper output key/value class is the same as the reducer output key/value class,
		// we need to set up separately
		job2.setMapOutputKeyClass(Text.class);
		job2.setMapOutputValueClass(Text.class);
		job2.setOutputKeyClass(DBOutputWritable.class);
		job2.setOutputValueClass(NullWritable.class);

		job2.setMapperClass(LanguageModel.Map.class);
		job2.setReducerClass(LanguageModel.Reduce.class);

		job2.setInputFormatClass(TextInputFormat.class);
		job2.setOutputFormatClass(DBOutputFormat.class);

		// Table's name is "output"
		// Table's structure is: first column - starting_phrase; second column - following_word; third column: count
		DBOutputFormat.setOutput(job2, "output",
				new String[] {"starting_phrase", "following_word", "count"});

		// Connect job1 to job2: set the output path of job1 tobe the input path of job2
		TextInputFormat.setInputPaths(job2, args[1]);
		job2.waitForCompletion(true);
	}

}
