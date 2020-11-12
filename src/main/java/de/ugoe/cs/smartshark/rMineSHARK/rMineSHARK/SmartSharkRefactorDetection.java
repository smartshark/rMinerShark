package de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK;

import java.io.FileWriter;
import java.util.*;

import com.mongodb.MongoClient;
import de.ugoe.cs.smartshark.rMineSHARK.util.Common;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import de.ugoe.cs.smartshark.model.Commit;
import de.ugoe.cs.smartshark.model.File;
import de.ugoe.cs.smartshark.model.FileAction;
import de.ugoe.cs.smartshark.model.Hunk;
import de.ugoe.cs.smartshark.model.Project;
import de.ugoe.cs.smartshark.model.VCSSystem;
import de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.internal.RefactoringTypeMatcher;
import de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.model.RefactoringHunk;
import de.ugoe.cs.smartshark.rMineSHARK.util.Logger;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.diff.CodeRange;

public class SmartSharkRefactorDetection {

    // Refactorings for oisafe
    // 3fe952c0c6ccd6371cbf727982a4e7f09e705707 ->
    // https://github.com/openintents/safe/commit/ed0c657a64f7c528722528320aa39b44d2addb79
    // ed0c657a64f7c528722528320aa39b44d2addb79
    // ef4a92c27d01a81534ce7f5368e864d0d0f25909

    private static final String TOOL_NAME = "rMiner";
    private static final String BEFORE_NAME = "D";
    private static final String AFTER_NAME = "A";
    private static final Map<String, String> typeMap;
    
    static {
    	typeMap = new HashMap<>();
    	typeMap.put("Rename Refactoring", "rename_refactoring");
    	typeMap.put("Rename Method", "rename_method");
    	typeMap.put("Move Class", "move_class");
    	typeMap.put("Change Return Type", "change_return_type");
    	typeMap.put("Inline Variable", "inline_variable");
    	typeMap.put("Inline Method", "inline_method");
    	typeMap.put("Extract Variable", "extract_variable");
    	typeMap.put("Extract And Move Method", "extract_and_move_method");
    	typeMap.put("Extract Method", "extract_method");
    	typeMap.put("Rename Variable", "rename_variable");
    	typeMap.put("Rename Attribute", "rename_attribute");
    	typeMap.put("Change Attribute Type", "change_variable_type");
    	typeMap.put("Change Variable Type", "change_variable_type");
    	typeMap.put("Extract Interface", "extract_interface");
    	typeMap.put("Rename Parameter", "rename_parameter");
    	typeMap.put("Move Method", "move_method");
    	typeMap.put("Parameterize Variable", "parametrize_variable");
    	typeMap.put("Pull Up Method", "pull_up_method");
    	typeMap.put("Rename Class", "rename_class");
    	typeMap.put("Change Parameter Type", "extract_attribute");
    	typeMap.put("Extract Attribute", "extract_attribute");
    	typeMap.put("Split Variable", "split_variable");
    	typeMap.put("Pull Up Attribute", "pull_up_attribute");
    	typeMap.put("Move Attribute", "move_attribute");
    	typeMap.put("Move And Rename Class", "move_and_rename_class");
    	typeMap.put("Extract Class", "extract_class");
    	typeMap.put("Move Source Folder", "move_source_folder");
    	typeMap.put("Replace Variable With Attribute", "replace_variable_with_attribute");
    	typeMap.put("Extract Superclass", "extract_superclass");
    	typeMap.put("Extract Subclass", "extract_subclass");
    	typeMap.put("Push Down Method", "push_down_method");
    	typeMap.put("Push Down Attribute", "push_down_attribute");
    	typeMap.put("Merge Variable", "merge_variable");
    	typeMap.put("Merge Parameter", "merge_parameter");
    	typeMap.put("Split Attribute", "split_attribute");
    	typeMap.put("Merge Attribute", "merge_attribute");
    	typeMap.put("Change Package", "change_package");
    	typeMap.put("Split Parameter", "split_parameter");
    	typeMap.put("Move And Rename Attribute", "move_and_rename_attribute");
    	typeMap.put("Replace Attribute", "replace_attribute");
    }
    
    private final MongoClient mongoClient;

    private int hunksRemoved = 0;
    private int completeFile = 0;
    private int completeLines = 0;
    private int commits = 0;

    private Project p;
    private Commit commit;

    private final Datastore datastore;
    private List<de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.model.Refactoring> refactoringsToStore;

    public SmartSharkRefactorDetection(Project project, Datastore datastore, MongoClient mongoClient) {
        this.p = project;
        this.datastore = datastore;
        refactoringsToStore = new ArrayList<>();
        this.mongoClient = mongoClient;
    }

    public SmartSharkRefactorDetection(Commit commit, Datastore datastore, MongoClient mongoClient) {
        this.commit = commit;
        this.datastore = datastore;
        refactoringsToStore = new ArrayList<>();
        this.mongoClient = mongoClient;
    }

    public void execute() throws Exception {

        if(p != null) {
            Query<VCSSystem> systems = datastore.createQuery(VCSSystem.class);
            systems.and(systems.criteria("projectId").equal(p.getId()));

            Common.loadRepoFromMongoDB(p.getName(), mongoClient);

            for (VCSSystem vcsSystem : systems) {
                performForVCSSystem(vcsSystem);
            }
        }

        if(commit != null)
        {
            p = findProjectOfCommit(commit);
            performForCommit(commit);
        }

        store(refactoringsToStore);
    }

    private Project findProjectOfCommit(Commit commit) {
        Query<VCSSystem> systems = datastore.createQuery(VCSSystem.class);
        systems.and(systems.criteria("id").equal(commit.getVcSystemId()));

        VCSSystem system = systems.get();

        Query<Project> project = datastore.createQuery(Project.class);
        project.and(project.criteria("id").equal(system.getProjectId()));

        return project.get();
    }

    private Repository loadRepo() throws Exception {
        GitService gitService = new GitServiceImpl();

        return gitService.openRepository("tmp/" + p.getName());
    }

    private void performForCommit(Commit commit) throws Exception {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        Repository repo = loadRepo();
        // Commit based refactoring

        miner.detectAtCommit(repo,
                commit.getRevisionHash(),
                new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
                        Logger.log("Refactorings at " + commitId);
                        // no refactoring -> return
                        if (refactorings.size() == 0)
                            return;
                        // find commit in mongo db
                        Commit commitDB = findCommitForId(commitId);
                        assert commitDB.getId() == commit.getId();
                        handleCommitAndRefactoring(commitDB, refactorings);
                        commits++;
                    }

                    public void handleException(String commitId, Exception e) {
                        e.printStackTrace();

                        Logger.log("Exeception for commit " + commitId);
                    }
                });

        shutdownHook();
    }

    private void performForVCSSystem(VCSSystem system) throws Exception {
        Logger.log("Analyze " + system.getUrl());
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        Repository repo = loadRepo();

        // Null to detect for *all* branches
        miner.detectAll(repo, (String) null, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                Logger.log("Refactorings at " + commitId);
                // no refactoring -> return
                if (refactorings.size() == 0)
                    return;
                // find commit in mongo db
                Commit commit = findCommitForId(commitId);
                if (commit != null) {
                    handleCommitAndRefactoring(commit, refactorings);
                    commits++;
                }
            }

            public void handleException(String commitId, Exception e) {
                e.printStackTrace();

                Logger.log("Exeception for commit " + commitId);
            }

        });
        shutdownHook();
    }

    protected void shutdownHook() throws Exception {
        // empty to override in if needed
    }

    private Commit findCommitForId(String name) {
        Query<Commit> commit = datastore.createQuery(Commit.class);
        commit.and(commit.criteria("revisionHash").equal(name));
        return commit.get();
    }

    private List<FileActionFileHunksContainer> findFileActionFileHunksContainerForCommit(Commit commit) {
        List<FileActionFileHunksContainer> fileActionFileHunksContainers = new ArrayList<>();
        Query<FileAction> fileAction = datastore.createQuery(FileAction.class);
        fileAction.and(fileAction.criteria("commitId").equal(commit.getId()));
        for (FileAction fileAction2 : fileAction) {

            Query<File> file = datastore.createQuery(File.class);
            file.and(file.criteria("id").equal(fileAction2.getFileId()));

            Query<Hunk> hunks = datastore.createQuery(Hunk.class);
            hunks.and(hunks.criteria("file_action_id").equal(fileAction2.getId()));

            fileActionFileHunksContainers
                    .add(new FileActionFileHunksContainer(fileAction2, file.get(), hunks.asList()));
        }
        return fileActionFileHunksContainers;
    }


    // HookMethod
    protected int handleCommitAndRefactoring(Commit commit, List<Refactoring> refactorings) {

        List<FileActionFileHunksContainer> fileActions = findFileActionFileHunksContainerForCommit(commit);

        // 1. Remove refactorings from hunks

        for (Refactoring ref : refactorings) {
            System.out.println(ref.getInvolvedClassesAfterRefactoring());
            List<List<CodeRange>> locations = RefactoringTypeMatcher.getLocationInfoRefactoringSpecific(ref);
            List<CodeRange> rangesBefore = locations.get(0);
            List<CodeRange> rangesAfter = locations.get(1);
            if (!rangesBefore.isEmpty() || !rangesAfter.isEmpty()) {
                de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.model.Refactoring refactoring = new de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.model.Refactoring();
                refactoring.setCommitId(commit.getId());
                refactoring.setDetectionTool(SmartSharkRefactorDetection.TOOL_NAME);
                refactoring.setType(typeMap.get(ref.getName()));
                refactoring.setDescription(ref.toString());
                refactoring.setHunks(new ArrayList<>());

                for (CodeRange codeRange : rangesBefore) {
                    // Sucht den passenden Container, der alle Hunks für die Location (gemachted wird über die Datei) enthält
                    FileActionFileHunksContainer matchedContainer = getContainerForLocation(codeRange, fileActions);
                    if (matchedContainer == null) {
                        Logger.log("No Match");
                        continue;
                    }
                    Logger.log("Match! " + ref.getRefactoringType());

                    // create hunk objects
                    int linesBefore = matchedContainer.getLines();
                    List<Hunk> affectedHunks = matchedContainer.getAffectedHunksBasedOnLocation(codeRange);
                    for (Hunk hunk : affectedHunks) {
                    	RefactoringHunk refactoringHunk = new RefactoringHunk();
                        refactoringHunk.setHunkId(hunk.getId());
                        refactoringHunk.setMode(SmartSharkRefactorDetection.BEFORE_NAME);
                        refactoringHunk.setStartColumn(codeRange.getStartColumn());
                        refactoringHunk.setStartLine(codeRange.getStartLine());
                        refactoringHunk.setEndColumn(codeRange.getEndColumn());
                        refactoringHunk.setEndLine(codeRange.getEndLine());
                        refactoring.getHunks().add(refactoringHunk);
                    }
                    //
                    //matchedContainer.printAreas();
                    Logger.log("LocationInfo: " + codeRange.getFilePath());
                    Logger.log(codeRange.getStartLine() + "----" + codeRange.getEndLine());
                    Logger.log("");
                    // hunksRemoved += matchedContainer.removeHunksBasedOnLocationInfo(locationInfo);
                    //int linesAfter = matchedContainer.getLines();
                    //completeLines += Math.max(0, linesBefore - linesAfter);
                }
                for (CodeRange codeRange : rangesAfter) {
                    // Sucht den passenden Container, der alle Hunks für die Location (gemachted wird über die Datei) enthält
                    FileActionFileHunksContainer matchedContainer = getContainerForLocation(codeRange, fileActions);
                    if (matchedContainer == null) {
                        Logger.log("No Match");
                        continue;
                    }
                    Logger.log("Match! " + ref.getRefactoringType());

                    // create hunk objects
                    int linesBefore = matchedContainer.getLines();
                    List<Hunk> affectedHunks = matchedContainer.getAffectedHunksBasedOnLocation(codeRange);
                    for (Hunk hunk : affectedHunks) {
                        RefactoringHunk refactoringHunk = new RefactoringHunk();
                        refactoringHunk.setHunkId(hunk.getId());
                        refactoringHunk.setMode(SmartSharkRefactorDetection.AFTER_NAME);
                        refactoringHunk.setStartColumn(codeRange.getStartColumn());
                        refactoringHunk.setStartLine(codeRange.getStartLine());
                        refactoringHunk.setEndColumn(codeRange.getEndColumn());
                        refactoringHunk.setEndLine(codeRange.getEndLine());
                        refactoring.getHunks().add(refactoringHunk);
                    }
                    //
                    //matchedContainer.printAreas();
                    Logger.log("LocationInfo: " + codeRange.getFilePath());
                    Logger.log(codeRange.getStartLine() + "----" + codeRange.getEndLine());
                    Logger.log("");
                    // hunksRemoved += matchedContainer.removeHunksBasedOnLocationInfo(locationInfo);
                    //int linesAfter = matchedContainer.getLines();
                    //completeLines += Math.max(0, linesBefore - linesAfter);
                }
                refactoringsToStore.add(refactoring);
            }
        }

        // 2. Remove whitespaces from hunks

        for (FileActionFileHunksContainer fileActionFileHunksContainer : fileActions) {
            if (fileActionFileHunksContainer.getFile().getPath().endsWith(".java")) {
                fileActionFileHunksContainer.cleanWhitespace();
                fileActionFileHunksContainer.cleanComments();
                fileActionFileHunksContainer.cleanAnnotation();
            }
        }

        // 3. Statistics
        int countHunksWithOutRefacorting = 0;
        for (FileActionFileHunksContainer fileActionFileHunksContainer : fileActions) {
            if (fileActionFileHunksContainer.getFile().getPath().endsWith(".java")) {
                if (fileActionFileHunksContainer.getHunks().size() == 0) {
                    completeFile += 1;
                    Logger.log(
                            "complete file as refactoring: " + fileActionFileHunksContainer.getFile().getPath());
                }
                for (HunkMapperObject hunk : fileActionFileHunksContainer.getHunks()) {
                    countHunksWithOutRefacorting += hunk.getNewLines();
                }
            }
        }
        return hunksRemoved;
    }

    public Project getP() {
        return p;
    }

    public Datastore getDatastore() {
        return datastore;
    }

    private FileActionFileHunksContainer getContainerForLocation(CodeRange codeRange,
                                                                 List<FileActionFileHunksContainer> fileActions) {
        Logger.log("Search for:" + codeRange.getFilePath());
        for (FileActionFileHunksContainer fileActionFileHunksContainer : fileActions) {
            if (fileActionFileHunksContainer.getFile().getPath().equals(codeRange.getFilePath())) {
                return fileActionFileHunksContainer;
            }
        }
        return null;
    }

    protected void store(List<de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.model.Refactoring> refactoringsToStore) {
        Logger.log("Should store " + refactoringsToStore.size() + " objects");
        datastore.save(refactoringsToStore);
        Logger.log("Ojects saved");
    }

    public int getHunksRemoved() {
        return hunksRemoved;
    }

    public int getCompleteFile() {
        return completeFile;
    }

    public int getCompleteLines() {
        return completeLines;
    }

    public int getCommits() {
        return commits;
    }
}
