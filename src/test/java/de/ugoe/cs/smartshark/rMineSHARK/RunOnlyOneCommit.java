package de.ugoe.cs.smartshark.rMineSHARK;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import de.ugoe.cs.smartshark.model.Commit;
import de.ugoe.cs.smartshark.model.Project;
import de.ugoe.cs.smartshark.model.VCSSystem;
import de.ugoe.cs.smartshark.rMineSHARK.local.LocalSmartSHARKRefactorDetection;
import de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.SmartSharkRefactorDetection;
import de.ugoe.cs.smartshark.rMineSHARK.util.Logger;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RunOnlyOneCommit {

   private static String[] commits = {"fb7aae4c64f7d2bf6dced00c49c3ffc428b2d572"};

   private static Datastore datastore;


    public static void main(String args[]) throws Exception {



        // Telling morphia where to find the models
        final Morphia morphia = new Morphia();
        morphia.mapPackage("de.ugoe.cs.smartshark.model");

        // Creating a connection to the database
        // local connection
        MongoClientURI uri = new MongoClientURI("mongodb://bledel:Q6QTZHhF@mongoshark.informatik.uni-goettingen.de:27017/?authSource=smartshark");
        MongoClient mongoClient = new MongoClient(uri);
        datastore = morphia.createDatastore(mongoClient, "smartshark");


        ThreadPoolExecutor executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(8);

        for (String project : commits) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Commit commit = findCommitForId(project);

                        SmartSharkRefactorDetection detection = new LocalSmartSHARKRefactorDetection(commit, datastore, mongoClient);
                        detection.execute();

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(e.getMessage());
                        System.out.println(project + " failed");
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);


    }


    private static void lineCountForProject(VCSSystem system, Datastore datastore, Project project) {
        LineCounter lineCounter = new LineCounter(system, datastore, project.getName());
        int count = lineCounter.getLinesOfAllCommits();
        Logger.log("All lines " + count);
    }

    private static Commit findCommitForId(String name) {
        Query<Commit> commit = datastore.createQuery(Commit.class);
        commit.and(commit.criteria("revisionHash").equal(name));
        return commit.get();
    }
}
