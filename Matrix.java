import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
public class Matrix
{
public static class Matrix_Mapper extends Mapper<LongWritable,Text,Text,Text>
{
/**
* Map function will collect/ group cell values required for
* calculating the output.
* @param key is ignored. Its just the byte offset
* @param value is a single line. (a, 0, 0, 63) (matrix name, row, column, value)
*
*/
@Override
protected void map(LongWritable key, Text value,Context context)
throws IOException, InterruptedException
{
String line = value.toString();
String[] entry = line.split(",");
String sKey = "";
String mat = entry[0].trim();
String row, col;
Configuration conf = context.getConfiguration();
String dimension = conf.get("dimension");
int dim = Integer.parseInt(dimension);

if(mat.matches("a"))
{
for (int i =0; i < dim ; i++) // hard coding matrix size 5
{
row = entry[1].trim(); // rowid
sKey = row+i;
context.write(new Text(sKey),value);
}
}
if(mat.matches("b"))
{
for (int i =0; i < dim ; i++)
{
col = entry[2].trim(); // colid
sKey = i+col;
context.write(new Text(sKey),value);
}
}
}
}
public static class Matrix_Reducer extends Reducer<Text, Text, Text, IntWritable>
{
/**
* Reducer does the actual matrix multiplication.
* @param key is the cell unique cell dimension (00) represents cell 0,0
* @value values required to calculate matrix multiplication result of that cell.
*/
@Override
protected void reduce(Text key, Iterable<Text> values,Context context)
throws IOException, InterruptedException
{
Configuration conf = context.getConfiguration();
String dimension = conf.get("dimension");
int dim = Integer.parseInt(dimension);
//System.out.println("Dimension from Reducer = " + dimension);
int[] row = new int[dim]; // hard coding as 5 X 5 matrix
int[] col = new int[dim];
for(Text val : values)
{
String[] entries = val.toString().split(",");
if(entries[0].matches("a"))
{
int index = Integer.parseInt(entries[2].trim());
row[index] = Integer.parseInt(entries[3].trim());
}
if(entries[0].matches("b"))
{
int index = Integer.parseInt(entries[1].trim());
col[index] = Integer.parseInt(entries[3].trim());
}
}
// Let us do matrix multiplication now..
int total = 0;
for(int i = 0 ; i < 5; i++)
{
total += row[i]*col[i];
}
context.write(key, new IntWritable(total));
}
}
public static void main(String[] args) throws IOException, ClassNotFoundException,
InterruptedException
{
Configuration conf = new Configuration();
conf.set("dimension", "5"); // set the matrix dimension here.
Job job = Job.getInstance(conf);
//conf.set("fs.defaultFS", "hdfs://localhost:54310"); // take this value from core-site.xml
FileSystem fs = FileSystem.get(conf);
job.setJarByClass(Matrix.class);
// Need to set this since, map out is different from reduce out
job.setMapOutputKeyClass(Text.class);
job.setMapOutputValueClass(Text.class);
job.setOutputKeyClass(Text.class);
job.setOutputValueClass(IntWritable.class);
job.setMapperClass(Matrix_Mapper.class);
job.setReducerClass(Matrix_Reducer.class);
job.setInputFormatClass(TextInputFormat.class);
job.setOutputFormatClass(TextOutputFormat.class);
Path input = new Path(args[0]);
Path output = new Path(args[1]);
fs.close();
FileInputFormat.addInputPath(job, input);
FileOutputFormat.setOutputPath(job, output);
job.waitForCompletion(true);
}
}
