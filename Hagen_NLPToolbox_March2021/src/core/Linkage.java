package core;
import java.util.*;
import java.io.*;
import java.lang.*;

import te.indexer.*;
import te.utils.*;
import JLanI.kernel.DataSourceException;
import JLanI.kernel.LanIKernel;
import JLanI.kernel.Request;
import JLanI.kernel.RequestException;
import JLanI.kernel.Response;

import de.uni_leipzig.asv.toolbox.baseforms.Zerleger2;
import de.uni_leipzig.asv.toolbox.viterbitagger.Tagger;
import de.uni_leipzig.asv.utils.Pretree;

import org.json.JSONArray; // JSON library from http://www.json.org/java/
import org.json.JSONObject;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.*;
//import org.neo4j.kernel.Traversal;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.fs.FileUtils;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;

public class Linkage {
	
	DatabaseManagementService managementService;
	GraphDatabaseService graphDB;
	
	Boolean directedRelationships = true;

	public Linkage(File satzFile) {
		
		org.neo4j.internal.unsafe.UnsafeUtil.disableIllegalAccessLogger();
		
		
        try (BufferedReader reader = new BufferedReader(new FileReader(satzFile))) {
            String line;
            while ((line = reader.readLine()) != null) {

        		Vector<String> stringVector = new Vector<>();
            	
        		// Find the indices of the '|' characters
        		 int firstPipeIndex = line.indexOf('|');
        	     int secondPipeIndex = line.indexOf('|', firstPipeIndex + 1);
        	     int thirdPipeIndex = line.indexOf('|', secondPipeIndex + 1);
        	     int forthPipeIndex = line.indexOf('|', thirdPipeIndex + 1);


        	         // Extract the two strings between the '|' characters
        	     if (firstPipeIndex >= 0 && secondPipeIndex > firstPipeIndex) {
        	         String firstString = line.substring(firstPipeIndex + 1, secondPipeIndex);
        	         String secondString = line.substring(thirdPipeIndex + 1, forthPipeIndex);
        	         int count = Integer.parseInt(line.substring(forthPipeIndex + 3, line.length() - 1));
        	         
                     
                     //System.out.println(firstString);
                     //System.out.println(secondString);
                     
                     // Store the pair of strings in the vector
                     stringVector.add(firstString);
                     stringVector.add(secondString);

                     // Add the Vectors to the DB
                     System.out.println(stringVector);
         			 addSentenceToCooccsDB(stringVector, count); // extract sentence co-occurrences (Neo4j DB)
         			 //updateDiceAndCosts();
                 	}
        	     }
            	updateDiceAndCosts();

		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void addSentenceToCooccsDB(Vector<String> words, int count) // adding words from a sentence to the graph DB
	{
		if (count != 0) {
		String db_path = System.getProperty("user.dir") + "/cooccsdatabase";
		File database = new File(db_path);
		
		managementService = new DatabaseManagementServiceBuilder(database).build();
		graphDB = managementService.database(DEFAULT_DATABASE_NAME);

		//int count = 0;
		int nodecount = 0;
		int relcount = 0;
		boolean f_flag = false;
		
		
		System.out.println("Co-occurrence database opened/created");
		try (Transaction tx = graphDB.beginTx()) {
			System.out.println("Adding co-occurrences...");
			
			//adding all nodes/terms
			for (Iterator<String> i = words.iterator(); i.hasNext();) {

				String word = i.next();
				f_flag = false;

				// checking if the node is already present
				ResourceIterator<Node> nodelist = tx.findNodes(Labels.SINGLE_NODE);
				while (nodelist.hasNext()) {
					Node wordnode = nodelist.next();
					if (wordnode.getProperty("name").equals(word)) {
						f_flag = true;
						nodecount = (int) wordnode.getProperty("occur");
						nodecount = nodecount + count;
						wordnode.setProperty("occur", nodecount);
						System.out.println("'" + word + "' node was already present. Count updated ("+nodecount+").");
					}

				}

				if (f_flag == false) {
					Node wordnode = tx.createNode(Labels.SINGLE_NODE);
					wordnode.setProperty("name", word);
					wordnode.setProperty("occur", count); // number of occurrences in database
					System.out.println("'" + word + "' node added. Count: "+count+"");
				}

			}
			

			boolean rel_found;
			
			//adding all co-occurrence relationships
			for (int p = 0; p < words.size(); p++) {
				for (int q = p + 1; q < words.size(); q++) {

					if (!(words.get(p)).equals(words.get(q))) {

						rel_found = false;
						Node n1 = tx.findNode(Labels.SINGLE_NODE, "name", words.get(p));
						Node n2 = tx.findNode(Labels.SINGLE_NODE, "name", words.get(q));

						// checking if relationship already exists
						Iterable<Relationship> allRelationships = n1.getRelationships();
						for (Relationship relationship : allRelationships) {
							if (directedRelationships) {
								if (n2.equals(relationship.getEndNode())) {
									relcount = (int) relationship.getProperty("count");
									relcount = relcount + count;
									relationship.setProperty("count", relcount);
									System.out.println("Directed relation already existed between nodes '" + words.get(p) + "' towards '"
											+ words.get(q) + "'. Count updated ("+relcount+").");
									rel_found = true;
									break;
								}
							}else {
								if (n2.equals(relationship.getOtherNode(n1))) {
									relcount = (int) relationship.getProperty("count");
									relcount = relcount + count;
									relationship.setProperty("count", relcount);
									System.out.println("Relation already existed between nodes '" + words.get(p) + "' and '"
											+ words.get(q) + "'. Count updated ("+relcount+").");
									rel_found = true;
									break;
									}
								}
							

						}

						// creating new relationship
						if (!rel_found) {
							Relationship relationship = n1.createRelationshipTo(n2, RelationshipTypes.IS_CONNECTED);
							relationship.setProperty("count", count);
							relationship.setProperty("dice", 0); // for calculating Dice ratio
							relationship.setProperty("cost", 0); // for different applications
							System.out.println("Relation inserted between nodes '" + words.get(p) + "' and '" + words.get(q)+"'. Count: "+count+"");
						}

					}

				}
			}

			tx.commit(); 
			System.out.println("DB access closed");
		} catch (Exception ex) {
        	
        	System.out.println("Exception occurred: " + ex.toString());
        	
        }
	
		managementService.shutdown();
		}
	}

	
	/**
	* Updates the Dice coefficient and costs for all relationships present in
	* the co-occurrence database
	* 
	*/
	public void updateDiceAndCosts() {
		int countA, countB, countAB;
		double dice;
		
		String db_path = System.getProperty("user.dir") + "/cooccsdatabase";
		File database = new File(db_path);

		managementService = new DatabaseManagementServiceBuilder(database).build();
		graphDB = managementService.database(DEFAULT_DATABASE_NAME);

		try (Transaction tx = graphDB.beginTx()) {
			ResourceIterator<Node> nodelist = tx.findNodes(Labels.SINGLE_NODE);
			while (nodelist.hasNext()) {
				
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
			}
			System.out.println("Update of Dice-values and costs finished.");
			tx.commit(); 
		} catch (Exception ex) {
        	
        	System.out.println("Exception occurred: " + ex.toString());
        	
        }
		
		managementService.shutdown();
	}
}
