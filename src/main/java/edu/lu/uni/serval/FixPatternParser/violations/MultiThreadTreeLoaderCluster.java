package edu.lu.uni.serval.FixPatternParser.violations;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import edu.lu.uni.serval.gumtree.GumTreeComparer;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalRegrouper;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.ListSorter;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by anilkoyuncu on 19/03/2018.
 */
public class MultiThreadTreeLoaderCluster {

    private static int resultType;

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }

    private static Logger log = LoggerFactory.getLogger(MultiThreadTreeLoaderCluster.class);

    public static void main(String[] args){

        String inputPath;
        String outputPath;
        String port;
        String pairsCSVPath;
        String importScript;
        if (args.length > 0) {
            inputPath = args[0];
            outputPath = args[1];
            port = args[2];
            pairsCSVPath = args[3];
            importScript = args[4];
        } else {
//            inputPath = "/Users/anilkoyuncu/bugStudy/dataset/GumTreeOutput2/";
            inputPath = "/Users/anilkoyuncu/bugStudy/code/python/clusterDumps";
            outputPath = "/Users/anilkoyuncu/bugStudy/dataset/";
            port = "6379";
            pairsCSVPath = "/Users/anilkoyuncu/bugStudy/dataset/pairs-csv/";
            importScript = "/Users/anilkoyuncu/bugStudy/dataset/redisSingleImport.sh";
        }

//        calculatePairsOfClusters(inputPath, outputPath);
        mainCompare(inputPath,port,pairsCSVPath,importScript);
        //        calculatePairs(inputPath, outputPath);
//        processMessages(inputPath,outputPath);
//        evaluateResults(inputPath,outputPath);

    }

    public static void loadRedis(String cmd, File f){
        Process process;
        log.info(f.getName());
        try {
            String comd = String.format(cmd, f.getAbsoluteFile());
            process = Runtime.getRuntime()

                    .exec(comd);


            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), System.out::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            assert exitCode == 0;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("Load done");
    }

    public static void mainCompare(String inputPath,String port,String pairsCSVPath,String importScript) {

        String cmd;
        cmd = "bash " + importScript +" %s";

        JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1",Integer.valueOf(port),20000000);


        File folder = new File(pairsCSVPath);
        File[] listOfFiles = folder.listFiles();
        Stream<File> stream = Arrays.stream(listOfFiles);
        List<File> folders = stream
                .filter(x -> !x.getName().startsWith("."))
                .collect(Collectors.toList());

        for (File f:folders){

            if(f.getName().startsWith("cluster76")) {


                try (Jedis jedis = jedisPool.getResource()) {
                    // do operations with jedis resource
                    ScanParams sc = new ScanParams();
                    sc.count(150000000);
                    sc.match("pair_[0-9]*");

                    log.info("Scanning");
                    ScanResult<String> scan = jedis.scan("0", sc);
                    int size = scan.getResult().size();

                    if (size == 0) {
                        loadRedis(cmd, f);

                        scan = jedis.scan("0", sc);
                        size = scan.getResult().size();

                    }
                    log.info("Scanned " + String.valueOf(size));


                    String clusterName = f.getName().replaceAll("[^0-9]+", "");


                    //76

                    scan.getResult().parallelStream()
                            .forEach(m -> coreCompare(m, inputPath, jedisPool, clusterName));


                    jedis.save();

                }
            }


        }



    }





    public  static Pair<ITree,String> getTree(String firstValue){
        String gumTreeInput = "/Volumes/data/bugStudy_backup/dataset/GumTreeInputBug4/";
        String[] split2 = firstValue.split("/");
        String cluster = split2[1];


        File folder = new File("/Users/anilkoyuncu/bugStudy/code/python/cluster/"+cluster);
        File[] listOfFiles = folder.listFiles();
        Stream<File> stream = Arrays.stream(listOfFiles);
        List<File> folders = stream
                .filter(x -> !x.getName().startsWith(".") && x.getName().startsWith(split2[2]))
                .collect(Collectors.toList());



        String[] split1 = folders.get(0).getName().split(".txt_");
        String s = split1[0];
        String[] splitPJ = split1[1].split("_");
        String project = splitPJ[1];
        String actionSetPosition = splitPJ[0];

        File prevFile = new File(gumTreeInput + project+ "/" + "prevFiles/prev_" + s + ".java");// previous file
        File revFile = new File(gumTreeInput  + project+ "/" + "revFiles/" + s + ".java");//rev file

        List<HierarchicalActionSet> actionSets = parseChangedSourceCodeWithGumTree2(prevFile, revFile);

        HierarchicalActionSet actionSet = actionSets.get(Integer.valueOf(actionSetPosition));
//        for (HierarchicalActionSet actionSet : actionSets) {



            ITree actionTree= null;
            ITree test2 = null;
        getActionTree(actionSet);
        ITree node = actionSet.getNode();
        List<ITree> descendants = node.getDescendants();
        for (ITree descendant : descendants) {
            if(descendant.getType() <= 100){
                descendant.setType(104);
            }
        }
        node.setParent(null);


        //        }
//        }

        Pair<ITree, String> pair = new Pair<>(node,project);
        return pair;
    }


    public static void getActionTree(HierarchicalActionSet actionSet){


        int newType = 0;

        Action action = actionSet.getAction();
        if (action instanceof Update){
            newType = 101;
        }else if(action instanceof Insert){
            newType =100;
        }else if(action instanceof Move){
            newType = 102;
        }else if(action instanceof Delete){
            newType=103;
        }else{
            new Exception("unknow action");
        }
        actionSet.getNode().setType(newType);
//        actionSet.getNode().setLabel("");
        List<HierarchicalActionSet> subActions = actionSet.getSubActions();
        if (subActions.size() != 0){
            for (HierarchicalActionSet subAction : subActions) {
                getActionTree(subAction);
            }


        }

    }
//    public static ITree getActionTree(HierarchicalActionSet actionSet, ITree parent, ITree children){
//
//        int newType = 0;
//
//        Action action = actionSet.getAction();
//        if (action instanceof Update){
//            newType = 101;
//        }else if(action instanceof Insert){
//            newType =100;
//        }else if(action instanceof Move){
//            newType = 102;
//        }else if(action instanceof Delete){
//            newType=103;
//        }else{
//            new Exception("unknow action");
//        }
//        if(actionSet.getParent() == null){
//            //root
//
//            parent = new Tree(newType,"");
//        }else{
//            children = new Tree(newType,"");
//            parent.addChild(children);
//        }
//        List<HierarchicalActionSet> subActions = actionSet.getSubActions();
//        if (subActions.size() != 0){
//            for (HierarchicalActionSet subAction : subActions) {
//
//                if(actionSet.getParent() == null){
//                    children = parent;
//                }
//                getActionTree(subAction,children,null);
//
//            }
//
//
//        }
//        return parent;
//    }


    
    private static void coreCompare(String name , String inputPath, JedisPool jedisPool,String clusterName) {


        try (Jedis jedis = jedisPool.getResource()) {


            Map<String, String> resultMap = jedis.hgetAll(name);

            resultMap.get("0");


            String[] split = name.split("_");
            String i = null;
            String j =null;
            try {
                 i = split[1];
                 j = split[2];
            }
            catch (Exception e){
                e.printStackTrace();
            }
            String firstValue = resultMap.get("0");
            String secondValue = resultMap.get("1");






//            firstValue = inputPath + firstValue;
//            secondValue = inputPath + secondValue;

//            String[] firstValueSplit = firstValue.split("/");
//            String[] secondValueSplit = secondValue.split("/");
//
//            if (firstValueSplit.length == 1) {
//                firstValue = inputPath + firstValueSplit[0];
//            } else {
//                firstValue = inputPath + firstValueSplit[1];
//            }
//
//            if (secondValueSplit.length == 1) {
//                secondValue = inputPath + secondValueSplit[0];
//            } else {
//                secondValue = inputPath + secondValueSplit[1];
//            }

            try {
                Pair<ITree, String> oldPair = getTree(firstValue);
                Pair<ITree, String> newPair = getTree(secondValue);

                ITree oldTree = oldPair.getValue0();
                ITree newTree = newPair.getValue0();

                String oldProject = oldPair.getValue1();
                String newProject = newPair.getValue1();



                Matcher m = Matchers.getInstance().getMatcher(oldTree, newTree);
                m.match();


                ActionGenerator ag = new ActionGenerator(oldTree, newTree, m.getMappings());
                ag.generate();
                List<Action> actions = ag.getActions();

                String resultKey = "result_" + (String.valueOf(i)) + "_" + String.valueOf(j);
                double chawatheSimilarity1 = m.chawatheSimilarity(oldTree, newTree);
                String chawatheSimilarity = String.format("%1.2f", chawatheSimilarity1);
                double diceSimilarity1 = m.diceSimilarity(oldTree, newTree);
                String diceSimilarity = String.format("%1.2f", diceSimilarity1);
                double jaccardSimilarity1 = m.jaccardSimilarity(oldTree, newTree);
                String jaccardSimilarity = String.format("%1.2f", jaccardSimilarity1);

                String editDistance = String.valueOf(actions.size());
//            jedis.select(1);
                String result = resultMap.get("0") + "," + oldProject +"," + resultMap.get("1") + "," +newProject+ "," + chawatheSimilarity + "," + diceSimilarity + "," + jaccardSimilarity + "," + editDistance;
//            jedis.set(resultKey, result);

                if (((Double) chawatheSimilarity1).equals(1.0) || ((Double) diceSimilarity1).equals(1.0)
                        || ((Double) jaccardSimilarity1).equals(1.0) || actions.size() == 0) {
                    String matchKey = "match-"+clusterName+"_" + (String.valueOf(i)) + "_" + String.valueOf(j);
                    jedis.select(1);
                    jedis.set(matchKey, result);
                }
                jedis.select(0);
                String pairKey = "pair_" + (String.valueOf(i)) + "_" + String.valueOf(j);
                jedis.del(pairKey);

//                log.info("Completed " + resultKey);

            }catch (Exception e){
                log.error(e.toString() + " {}",(name));


            }




        }
    }



    protected static List<HierarchicalActionSet> parseChangedSourceCodeWithGumTree2(File prevFile, File revFile) {
        List<HierarchicalActionSet> actionSets = new ArrayList<>();
        // GumTree results
        List<Action> gumTreeResults = new GumTreeComparer().compareTwoFilesWithGumTree(prevFile, revFile);
        if (gumTreeResults == null) {
            resultType = 1;
            return null;
        } else if (gumTreeResults.size() == 0){
            resultType = 2;
            return actionSets;
        } else {
            // Regroup GumTre results.
            List<HierarchicalActionSet> allActionSets = new HierarchicalRegrouper().regroupGumTreeResults(gumTreeResults);
//			for (HierarchicalActionSet actionSet : allActionSets) {
//				String astNodeType = actionSet.getAstNodeType();
//				if (astNodeType.endsWith("Statement") || "FieldDeclaration".equals(astNodeType)) {
//					actionSets.add(actionSet);
//				}
//			}

            // Filter out modified actions of changing method names, method parameters, variable names and field names in declaration part.
            // variable effects range, sub-actions are these kinds of modification?
//			actionSets.addAll(new ActionFilter().filterOutUselessActions(allActionSets));

            ListSorter<HierarchicalActionSet> sorter = new ListSorter<>(allActionSets);
            actionSets = sorter.sortAscending();

            if (actionSets.size() == 0) {
                resultType = 3;
            }

            return actionSets;
        }
    }


/*
orginal calculate pairs, from all dumps of the projects
 */
    public static void calculatePairs(String inputPath, String outputPath) {
        File folder = new File(inputPath);
        File[] listOfFiles = folder.listFiles();
        Stream<File> stream = Arrays.stream(listOfFiles);
        List<File> pjs = stream
                .filter(x -> !x.getName().startsWith("."))
                .collect(Collectors.toList());

        List<File> fileToCompare = new ArrayList<>();
        for (File pj : pjs) {
            File[] files = pj.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith("ASTDumps");
                }
            });
            Collections.addAll(fileToCompare, files[0].listFiles());

        }
        System.out.println("a");
//        compareAll(fileToCompare);
        readMessageFiles(fileToCompare, outputPath);
    }

    /*
    pairs of each cluster
     */
    public static void calculatePairsOfClusters(String inputPath, String outputPath) {
        File folder = new File(inputPath);
        File[] listOfFiles = folder.listFiles();
        Stream<File> stream = Arrays.stream(listOfFiles);
        List<File> pjs = stream
                .filter(x -> !x.getName().startsWith("."))
                .collect(Collectors.toList());

        FileHelper.createDirectory(outputPath + "pairs/");
        for (File pj : pjs) {
            File[] files = pj.listFiles();
            List<File> fileList = Arrays.asList(files);

            readMessageFilesCluster(fileList, outputPath,inputPath,pj.getName());

        }

    }

    public static void processMessages(String inputPath, String outputPath) {
        File folder = new File(outputPath + "pairs_splitted/");
        File[] listOfFiles = folder.listFiles();
        Stream<File> stream = Arrays.stream(listOfFiles);
        List<File> pjs = stream
                .filter(x -> !x.getName().startsWith("."))
                .collect(Collectors.toList());
        FileHelper.createDirectory(outputPath + "comparison_splitted/");
        pjs.parallelStream()
                .forEach(m -> coreLoop(m, outputPath,inputPath));
    }

    private static void readMessageFilesCluster(List<File> folders, String outputPath,String inputPath,String cluster) {

        List<String> treesFileNames = new ArrayList<>();


        for (File target : folders) {

            treesFileNames.add(target.toString());
        }

        log.info("Calculating pairs");
//        treesFileNames = treesFileNames.subList(0,100);

        String filename = "cluster" + cluster;
        byte [] buf = new byte[0];
        String line = null;
        try {

            FileOutputStream fos = new FileOutputStream(outputPath + "pairs/" +filename+".txt");
            DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos));



            for (int i = 0; i < treesFileNames.size(); i++) {
                for (int j = i + 1; j < treesFileNames.size(); j++) {



                    line = String.valueOf(i) +"\t" + String.valueOf(j) + "\t" + treesFileNames.get(i).replace(inputPath,"") + "\t" + treesFileNames.get(j).replace(inputPath,"")+"\n";
                    outStream.write(line.getBytes());

                }
            }
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (java.nio.BufferOverflowException e) {
            log.error(line);
            log.error(String.valueOf(buf.length));
            e.printStackTrace();
        }

        log.info("Done pairs");
    }



    public static ITree getSimpliedTree(String fn) {
        ITree tree = null;
        try {
            FileInputStream fi = new FileInputStream(new File(fn));
            ObjectInputStream oi = new ObjectInputStream(fi);
            tree = (ITree) oi.readObject();
            oi.close();
            fi.close();


        } catch (FileNotFoundException e) {
            log.error("File not found");
            e.printStackTrace();
        } catch (IOException e) {
            log.error("Error initializing stream");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

//        tree.setLabel("");
        tree.setParent(null);
//        List<ITree> descendants = tree.getDescendants();
//        for (ITree descendant : descendants) {
//            descendant.setLabel("");
//        }

        return tree;

    }


    private static void coreLoop(File mes, String outputPath,String inputPath) {
        try {

            log.info("Starting in coreLoop");

            BufferedReader br = null;
            String sCurrentLine = null;
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "comparison_splitted/" + "output_" + mes.getName()));

                br = new BufferedReader(
                        new FileReader(mes));
                while ((sCurrentLine = br.readLine()) != null) {
                    String currentLine = sCurrentLine;
                    String[] split = currentLine.split("\t");
                    String i = split[0];
                    String j = split[1];
                    String firstValue = split[2];
                    String secondValue = split[3];

                    firstValue = inputPath + firstValue.split("GumTreeOutput2")[1];
                    secondValue = inputPath + secondValue.split("GumTreeOutput2")[1];

                    ITree oldTree = getSimpliedTree(firstValue);

                    ITree newTree = getSimpliedTree(secondValue);

                    Matcher m = Matchers.getInstance().getMatcher(oldTree, newTree);
                    m.match();

                    ActionGenerator ag = new ActionGenerator(oldTree, newTree, m.getMappings());
                    ag.generate();
                    List<Action> actions = ag.getActions();
                    writer.write(String.valueOf(i));
                    writer.write("\t");
                    writer.write(String.valueOf(j));
                    writer.write("\t");

                    writer.write(String.format("%1.2f", m.chawatheSimilarity(oldTree, newTree)));
                    writer.write("\t");
                    writer.write(String.format("%1.2f", m.diceSimilarity(oldTree, newTree)));
                    writer.write("\t");
                    writer.write(String.format("%1.2f", m.jaccardSimilarity(oldTree, newTree)));
                    writer.write("\t");
                    writer.write(String.valueOf(actions.size()));
                    writer.write("\t");
                    writer.write(firstValue);
                    writer.write("\t");
                    writer.write(secondValue);
                    writer.write("\n");


                }
            writer.close();
        } catch (FileNotFoundException e) {
            log.error("File not found");
            e.printStackTrace();
        } catch (IOException e) {
            log.error("Error initializing stream");
            e.printStackTrace();

        }
        log.info("Completed output_" + mes.getName());
    }

    private static void readMessageFiles(List<File> folders, String outputPath) {

        List<String> treesFileNames = new ArrayList<>();


        for (File target : folders) {

            treesFileNames.add(target.toString());
        }
        FileHelper.createDirectory(outputPath + "pairs/");
        log.info("Calculating pairs");
//        treesFileNames = treesFileNames.subList(0,100);
        byte [] buf = new byte[0];
        String line = null;
        try {

            FileChannel rwChannel = new RandomAccessFile(outputPath + "pairs/" +"textfile.txt", "rw").getChannel();
            ByteBuffer wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, Integer.MAX_VALUE);
            int fileCounter = 0;


            for (int i = 0; i < treesFileNames.size(); i++) {
                for (int j = i + 1; j < treesFileNames.size(); j++) {



                     line = String.valueOf(i) +"\t" + String.valueOf(j) + "\t" + treesFileNames.get(i).replace("/Users/anilkoyuncu/bugStudy/dataset/GumTreeOutput2","") + "\t" + treesFileNames.get(j).replace("/Users/anilkoyuncu/bugStudy/dataset/GumTreeOutput2","")+"\n";
                     buf  = line.getBytes();
                    if(wrBuf.remaining() > 500) {
                        wrBuf.put(buf);
                    }else{
                        log.info("Next pair dump");
                        fileCounter++;
                        rwChannel = new RandomAccessFile(outputPath+"pairs/" +"textfile"+String.valueOf(fileCounter)+".txt", "rw").getChannel();
                        wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, Integer.MAX_VALUE);
                    }




                }
            }
            rwChannel.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (java.nio.BufferOverflowException e) {
            log.error(line);
            log.error(String.valueOf(buf.length));
            e.printStackTrace();
        }

        log.info("Done pairs");
    }

    static final JedisPoolConfig poolConfig = buildPoolConfig();


    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofMinutes(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofHours(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);

        return poolConfig;
    }



//        return msgFiles;
}



