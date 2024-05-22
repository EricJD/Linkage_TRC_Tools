package core;
import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import te.indexer.*;
import te.utils.*;
import JLanI.kernel.DataSourceException;
import JLanI.kernel.LanIKernel;
import JLanI.kernel.Request;
import JLanI.kernel.RequestException;
import JLanI.kernel.Response;
import core.GCC.NodeComparator;
import de.uni_leipzig.asv.toolbox.baseforms.Zerleger2;
import de.uni_leipzig.asv.toolbox.viterbitagger.Tagger;
import de.uni_leipzig.asv.utils.Pretree;

import org.glassfish.jersey.internal.guava.Iterators;
import org.json.JSONArray; // JSON library from http://www.json.org/java/
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.*;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.io.fs.FileUtils;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;

public class MergeDB {

	
	DatabaseManagementService managementService;
	GraphDatabaseService graphDB;

	String DBDirPath = "cooccsdatabase/";
	
	String filenameNodes = "Nodes.txt"; 
	String filenameRel = "Relationships.txt"; 
	
	String NodesPath = DBDirPath + File.separator + filenameNodes;
	String RelPath = DBDirPath + File.separator + filenameRel;

	public MergeDB() {
		org.neo4j.internal.unsafe.UnsafeUtil.disableIllegalAccessLogger();
		
		String db_path = System.getProperty("user.dir") + "/cooccsdatabase";
		File database = new File(db_path);
		
		managementService = new DatabaseManagementServiceBuilder(database).build();
		graphDB = managementService.database(DEFAULT_DATABASE_NAME);
		File DBDir = new File(DBDirPath);

	}
	
	public void backupDB() 
	{
		
		//String db_path = System.getProperty("user.dir") + "/cooccsdatabase";
		//File database = new File(db_path);
		
		//managementService = new DatabaseManagementServiceBuilder(database).build();
		//graphDB = managementService.database(DEFAULT_DATABASE_NAME);
		
		try (Transaction tx = graphDB.beginTx()) {
		System.out.println("DB open");
	

	    BufferedWriter writerNodes = new BufferedWriter(new FileWriter(NodesPath, true));
	    
	    System.out.println("Starting to write nodes");
	    
	    ResourceIterator<Node> nodelist = tx.findNodes(Labels.SINGLE_NODE);	
		while (nodelist.hasNext()) {
			Node node = nodelist.next();
			String name = (String) node.getProperty("name");
			int occur = (int) node.getProperty("occur");
			
			writerNodes.write("|" + name + "|" + occur + "|\n");
			//System.out.println("Saving |" + name + "|" + occur + "|");
			
		//	if (wordnode.getProperty("name").equals(word)) {
		//		f_flag = true;
		//		nodecount = (int) wordnode.getProperty("occur");
		//		nodecount = nodecount + count;
		//		wordnode.setProperty("occur", nodecount);
		//		System.out.println("'" + word + "' node was already present. Count updated ("+nodecount+").");
		//	}
			
		}
		writerNodes.flush();
		//get: name and occur
		System.out.println("Nodes fninished");
		
		
	    BufferedWriter writerRel = new BufferedWriter(new FileWriter(RelPath, true));
	    
	    System.out.println("Starting to write relationships");
	    
	    ResourceIterable<Relationship> rellist = tx.getAllRelationships();
		for (Relationship rel : rellist) {
		String StartNode = (String) rel.getStartNode().getProperty("name");
		String EndNode = (String) rel.getEndNode().getProperty("name");
		int count = (int) rel.getProperty("count");
		
		writerRel.write("|" + StartNode + "|" + EndNode + "|" + count + "|\n");
		//System.out.println("Saving |" + StartNode + "|" + EndNode + "|" + count + "|");
		
		//get: StartNode, EndNode, count

		}
		writerRel.flush();
		System.out.println("Relationsships finished");
		} catch (Exception ex) {
        	
        	System.out.println("Exception occurred: " + ex.toString());
        	
		}
		
		System.out.println("Closing DB");
		managementService.shutdown();
		System.out.println("All done, DB saved!");
	}
	
	public void merge() {

			Boolean f_flag = false;
			Boolean rel_found = true;
			String name  = "";
			int occur = 0;
			String startNode = "";
			String endNode = "";
			int count = 0;
			int relcount = 0;
			int nodecount = 0;
	
			System.out.println("Co-occurrence database opened/created");
			try (Transaction tx = graphDB.beginTx()) {
				System.out.println("Adding co-occurrences...");
				
				System.out.println("Merging nodes started");
				BufferedReader NodeReader = new BufferedReader(new FileReader(NodesPath));
	            String lineNode;
	            
	            
	            while ((lineNode = NodeReader.readLine()) != null) {
	            	f_flag = false;
	            	
	                String[] partsNodes = lineNode.split("\\|");
	                if (partsNodes.length == 3) { // Assuming each line has the format "|String|int|"
	                    name = partsNodes[1];
	                    occur = Integer.parseInt(partsNodes[2]);

	                    //System.out.println("String: " + name);
	                    //System.out.println("Int: " + occur);
	                    
	                    ResourceIterator<Node> nodelist = tx.findNodes(Labels.SINGLE_NODE);
						// checking if the node is already present
						while (nodelist.hasNext()) {
							Node wordnode = nodelist.next();
							if (wordnode.getProperty("name").equals(name)) {
								f_flag = true;
								nodecount = (int) wordnode.getProperty("occur");
								nodecount = nodecount + occur;
								wordnode.setProperty("occur", nodecount);
								System.out.println("'" + name + "' node was already present. Count updated ("+nodecount+").");
								break;
							}

						}
					

						if (f_flag == false) {
							Node wordnode = tx.createNode(Labels.SINGLE_NODE);
							wordnode.setProperty("name", name);
							wordnode.setProperty("occur", occur); // number of occurrences in database
							System.out.println("'" + name + "' node added. Count: "+occur+"");
	
						}
					
	                }

				
	                }
				System.out.println("Merging nodes finished");
	            
	            
				System.out.println("Merging relationships started");
				BufferedReader RelReader = new BufferedReader(new FileReader(RelPath));
	            String lineRel;
	            //ResourceIterable<Relationship> rellist = tx.getAllRelationships();

	            
	            while ((lineRel = RelReader.readLine()) != null) {
					rel_found = false;
					//System.out.println(lineRel);
	            	
	                String[] partsRel = lineRel.split("\\|");
	                if (partsRel.length == 4) { // Assuming each line has the format "|String|String|int|"
	                	//System.out.println("Reading done");
	                    startNode = partsRel[1];
	                    endNode = partsRel[2];
	                    count = Integer.parseInt(partsRel[3]);
	                    
	                 // checking if relationship already exists
	    	            ResourceIterator<Node> nodelistrel = tx.findNodes(Labels.SINGLE_NODE);
						while (nodelistrel.hasNext()) {
							Node wordnode = nodelistrel.next();
							//System.out.println(wordnode.getProperty("name"));
							//System.out.println(startNode);
							if (wordnode.getProperty("name").equals(startNode)) {
								//System.out.println("Start Node found");
								Iterable<Relationship> allRelationships = wordnode.getRelationships();
								for (Relationship relationship : allRelationships) {
									if (endNode.equals(relationship.getEndNode().getProperty("name"))) {
										relcount = (int) relationship.getProperty("count");
										relcount = relcount + count;
										relationship.setProperty("count", relcount);
										System.out.println("Directed relation already existed between nodes '" + wordnode.getProperty("name") + "' towards '"
												+ relationship.getEndNode().getProperty("name") + "'. Count updated ("+relcount+").");
										rel_found = true;
										break;
									}
								}

								// creating new relationship
								if (!rel_found) {
									Relationship relationship = wordnode.createRelationshipTo(tx.findNode(Labels.SINGLE_NODE, "name", endNode), RelationshipTypes.IS_CONNECTED);
									relationship.setProperty("count", count);
									relationship.setProperty("dice", 0); // for calculating Dice ratio
									relationship.setProperty("cost", 0); // for different applications
									System.out.println("Relation inserted between nodes '" + wordnode.getProperty("name") + "' and '" + relationship.getEndNode().getProperty("name")+"'. Count: "+count+"");
									break;
								}
							}

						}

	                	}
	            }
				System.out.println("Merging relationships finished");
				tx.commit(); 
				
				System.out.println("DB access closed");
				managementService.shutdown();

			} catch (Exception ex) {
	        	
	        	System.out.println("Exception occurred: " + ex.toString());
	        	
	        }
		

		}
		
	
	public void updateDiceAndCosts() {
		int countA, countB, countAB;
		double dice;
		
		String db_path = System.getProperty("user.dir") + "/cooccsdatabase";
		File database = new File(db_path);

		managementService = new DatabaseManagementServiceBuilder(database).build();
		graphDB = managementService.database(DEFAULT_DATABASE_NAME);

		long pos = 0;
		Boolean updating = true;
		
		System.out.println("Update of Dice-values and costs started.");
		
		while (updating) {
		try (Transaction tx = graphDB.beginTx()) {
			//System.out.println("Get Nodes.");
			ResourceIterator<Node> nodelist = tx.findNodes(Labels.SINGLE_NODE);
			long n = 0;
			int counter = 0;
			System.out.println("Start iterating.");
			while (nodelist.hasNext() && counter < 500) {
				if(n >= pos) {
				//System.out.println("Start round.");
				Node n1 = nodelist.next();
				countA = (int) n1.getProperty("occur");
				
				Iterable<Relationship> allRelationships = n1.getRelationships();
				for (Relationship relationship : allRelationships) {
					Node n2 = relationship.getOtherNode(n1);
										
					countB = (int) n2.getProperty("occur");
					countAB = (int) relationship.getProperty("count");

					dice = (double) (2 * countAB) / (countA + countB);
					
					relationship.setProperty("dice", dice);
					relationship.setProperty("cost", 1 / (dice + 0.01));
					
					
					}
				n++;
				counter++;
				}
				else {
					//System.out.println("Skip round.");
					n++;
				}
			}

			pos = n;
			long size = Iterators.size(nodelist);
			if(pos >= size) {
				updating = false;
			}
			else {
				System.out.println("pos = " + pos);
				System.out.println("size = " + size);
			}
			System.out.println("Update of Dice-values and costs saved.");
			tx.commit(); 
		
		} catch (Exception ex) {
        	
        	System.out.println("Exception occurred: " + ex.toString());
        	
        }
		}
		System.out.println("Update of Dice-values and costs finished.");
		managementService.shutdown();
	}


	
	public static void main(String[] args) {
		
		int mode = 2; // 1 = save DB, 2 = merge DB with save files
		
		MergeDB MergeDB = new MergeDB();
		
		if(mode == 1) {
			long start = System.currentTimeMillis();

			MergeDB.backupDB();

			long end = System.currentTimeMillis();

			System.out.println("Processing took " + (end - start) / 1000 + " seconds.");
		}
		
		if(mode == 2) {
			long start = System.currentTimeMillis();

			MergeDB.merge();
			MergeDB.updateDiceAndCosts();

			long end = System.currentTimeMillis();

			System.out.println("Processing took " + (end - start) / 1000 + " seconds.");
		}


	
	}

}



