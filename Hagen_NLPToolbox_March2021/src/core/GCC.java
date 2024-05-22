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

import de.uni_leipzig.asv.toolbox.baseforms.Zerleger2;
import de.uni_leipzig.asv.toolbox.viterbitagger.Tagger;
import de.uni_leipzig.asv.utils.Pretree;

import org.json.JSONArray; // JSON library from http://www.json.org/java/
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.*;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.io.fs.FileUtils;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;

public class GCC {
	
	DatabaseManagementService managementService;
	GraphDatabaseService graphDB;

	public GCC() {
		org.neo4j.internal.unsafe.UnsafeUtil.disableIllegalAccessLogger();
		
		String db_path = System.getProperty("user.dir") + "/cooccsdatabase";
		File database = new File(db_path);
		
		managementService = new DatabaseManagementServiceBuilder(database).build();
		graphDB = managementService.database(DEFAULT_DATABASE_NAME);

	}	
	
	public void getRel() // adding words from a sentence to the graph DB
	{
		
		//String db_path = System.getProperty("user.dir") + "/cooccsdatabase";
		//File database = new File(db_path);
		
		//managementService = new DatabaseManagementServiceBuilder(database).build();
		//graphDB = managementService.database(DEFAULT_DATABASE_NAME);
		
		try (Transaction tx = graphDB.beginTx()) {
			System.out.println("DB open");
			
		Node n1 = tx.findNode(Labels.SINGLE_NODE, "name", "Surface runoff");
		Node n2 = tx.findNode(Labels.SINGLE_NODE, "name", "surface water");
		Node n3 = tx.findNode(Labels.SINGLE_NODE, "name", "6PPD");

		// checking if relationship already exists
		Iterable<Relationship> allRelationships = n1.getRelationships();
		
		System.out.println(allRelationships);
		
		for (Relationship relationship : allRelationships) {
			System.out.println(relationship.getOtherNode(n1));
			
			if (n2.equals(relationship.getOtherNode(n1))) {
				System.out.println("found 'surface water'");
				System.out.println("End Node: " + relationship.getEndNode());
			}
			if (n3.equals(relationship.getOtherNode(n1))) {
				System.out.println("found '6PPD'");
			}
			
			if (n2.equals(relationship.getStartNode())) {
				System.out.println("adding relationship for 'surface water'");
				System.out.println("adding relationship for '6PPD'");
				Relationship relationshipNew1 = n1.createRelationshipTo(n2, RelationshipTypes.IS_CONNECTED);
				relationship.setProperty("count", 1);
				relationship.setProperty("dice", 0); // for calculating Dice ratio
				relationship.setProperty("cost", 0); // for different applications
				System.out.println("Relation inserted between nodes \"Surface runoff\" and \"surface water\".");
			}
			if (n3.equals(relationship.getStartNode())) {
				System.out.println("adding relationship for '6PPD'");
				Relationship relationshipNew2 = n1.createRelationshipTo(n3, RelationshipTypes.IS_CONNECTED);
				relationship.setProperty("count", 1);
				relationship.setProperty("dice", 0); // for calculating Dice ratio
				relationship.setProperty("cost", 0); // for different applications
				System.out.println("Relation inserted between nodes \"Surface runoff\" and \"6PPD\".");
			}
			 

		}
		tx.commit();
		
		//for (Relationship relationship : allRelationships) {
		//	if (n2.equals(relationship.getOtherNode(n1))) {
		//		count = (int) relationship.getProperty("count");
		//		count = count + 1;
		//		relationship.setProperty("count", count);
		//		System.out.println("Relation already existed between nodes '" + words.get(p) + "' and '"
		//				+ words.get(q) + "'. Count updated.");
		//		rel_found = true;
		//		break;
		//		}

			}

		// creating new relationship
		//if (!rel_found) {
		//	Relationship relationship = n1.createRelationshipTo(n2, RelationshipTypes.IS_CONNECTED);
		//	relationship.setProperty("count", 1);
		//	relationship.setProperty("dice", 0); // for calculating Dice ratio
		//	relationship.setProperty("cost", 0); // for different applications
		//	System.out.println("Relation inserted between nodes '" + words.get(p) + "' and '" + words.get(q)+"'.");
		//	}
		//}
		
	}
	
	public void writeSetToFile(Set<?> set, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Object element : set) {
                writer.write(element.toString());
                writer.newLine();
            }
            System.out.println("Set contents written to file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }	
	
	public boolean dfs(Node node, Set<Node> visited, Set<Node> component) {
	    visited.add(node);

	    for (Relationship rel : node.getRelationships()) {

	    	Node neighbor = rel.getEndNode();
	    	if (!neighbor.equals(node)) {
		        if (component.contains(neighbor)) {
		        	component.add(node);
		        	return true;
		        }else if  (!visited.contains(neighbor)) {
	        	   	if(dfs(neighbor, visited, component)) {
	    	        	component.add(node);
	    	        	return true;
	        	   	}
		        }
	    	}
	    }
		return false;
	}

	public Set<Node> findLargestStronglyConnectedComponent() {
		Set<Node> largestComponent = new HashSet<>();
	    //Set<Node> visited = new HashSet<>();


	    
		//String db_path = System.getProperty("user.dir") + "/cooccsdatabase";
		//File database = new File(db_path);
		
		//managementService = new DatabaseManagementServiceBuilder(database).build();
		//graphDB = managementService.database(DEFAULT_DATABASE_NAME);	    

	    try (Transaction tx = graphDB.beginTx()) {
	        for (Node node : tx.getAllNodes()) {
	        	System.out.println("Starting run with "+ node);
	            //if (!visited.contains(node)) {
	        	Set<Node> component = new HashSet<>();
	        	Set<Node> visited = new HashSet<>();
	            component.add(node);
	            dfs(node, visited, component);

	            if (component.size() > largestComponent.size()) {
	                largestComponent = component;

	                //}
	            }
	        }
	        tx.commit();
	    }

	    return largestComponent;
	}
	
	public void deleteNodesAndRelationshipsOutsideLargestComponent(Set<Node> largestComponent) {
	    //Set<Node> largestComponent = findLargestStronglyConnectedComponent();
	    
	    //System.out.println("Nodes in SCC (Method): " + findLargestStronglyConnectedComponent());
	    //System.out.println("Nodes in SCC: " + largestComponent);
	    
	    Set<Long> largestComponentNodeIds = largestComponent.stream()
	        .map(Node::getId)
	        .collect(Collectors.toSet());
	    
	    //for debugging
        writeSetToFile(largestComponentNodeIds, "C:\\Users\\EricD\\Desktop\\Neuer Ordner\\output.txt");

	    //System.out.println("SCC IDs: " + largestComponentNodeIds);
	    long d = 0;
	    try (Transaction tx = graphDB.beginTx()) {
	    	Set<Long> NodeIds = tx.getAllNodes().stream()
	    	        .map(Node::getId)
	    	        .collect(Collectors.toSet());
	    	
	    //for debugging
	    writeSetToFile(NodeIds, "C:\\Users\\EricD\\Desktop\\Neuer Ordner\\output2.txt");
	    	
	        for (Relationship rel : tx.getAllRelationships()) {
	            Node startNode = rel.getStartNode();
	            Node endNode = rel.getEndNode();
	            //System.out.println("StartNode: " + startNode);
	            //System.out.println("EndNode: " + endNode);
	            if (!largestComponentNodeIds.contains(startNode.getId()) || 
	                !largestComponentNodeIds.contains(endNode.getId())) {
	                // Relationship is not part of the largest component, delete it.
	                rel.delete();
	                System.out.println("Deleted Relationship: " + rel);
	            }
	        }
	        
	    	for (Node node : tx.getAllNodes()) {
	    		//System.out.println("Checking if Node " + node.getId() + " is in the SCC");
	            if (!largestComponentNodeIds.contains(node.getId())) {
	                // Node is not part of the largest component, delete it.
	            	//System.out.println("Yes, deleting Node");
	                node.delete();
	                System.out.println("Deleted Node: " + node);
	                d++;
	            }
	        }
	    	//System.out.println(tx.getAllNodes());
	    	tx.commit();
	    	System.out.println(d + "nodes deleted.");
	        System.out.println("All nodes and relationships outside the largest SCC deleted.");
	    }
	}
	
	
	
/*	
	static class Node2 {
	    private int value;

	    public Node2(int value) {
	        this.value = value;
	    }

	    // Override equals and hashCode methods to define Node equality
	    @Override
	    public boolean equals(Object obj) {
	        if (this == obj) {
	            return true;
	        }
	        if (obj == null || getClass() != obj.getClass()) {
	            return false;
	        }
	        Node2 node2 = (Node2) obj;
	        return value == node2.value;
	    }

	    @Override
	    public int hashCode() {
	        return Objects.hash(value);
	    }

	    @Override
	    public String toString() {
	        return "Node2{" +
	                "value=" + value +
	                '}';
	    }
	}
*/	
	
	static class NodeComparator implements Comparator<Node> {
	    @Override
	    public int compare(Node node1, Node node2) {
	        // Define the sorting criteria (ascending order based on 'value')
	        return Long.compare(node1.getId(), node2.getId());
	    }
	}
	
	public static void main(String[] args) {
		
		GCC GCC = new GCC();
		//GCC.getRel();	
		
		//System.out.println(GCC.findLargestStronglyConnectedComponent());	
		Set<Node> LargestComponent = GCC.findLargestStronglyConnectedComponent();
		
		List<Node> sortedList = new ArrayList<>(LargestComponent);
		Collections.sort(sortedList, new NodeComparator());
		
		System.out.println("Sorted List: " + sortedList);
		System.out.println("List size: " + sortedList.size());

		
		
		
		//GCC.deleteNodesAndRelationshipsOutsideLargestComponent(LargestComponent);

		
		
		/*
        Set<Node2> nodeSet = new HashSet<>();

        // Add elements to the set (including duplicates)
        nodeSet.add(new Node2(1));
        nodeSet.add(new Node2(2));
        nodeSet.add(new Node2(1)); // Duplicate
        nodeSet.add(new Node2(3));
        nodeSet.add(new Node2(2)); // Duplicate

        // Print the unique elements
        System.out.println("nodeSet");
        for (Node2 node2 : nodeSet) {
            System.out.println(node2);
        }
        
        // Convert the Set to a List
        List<Node2> nodeList = new ArrayList<>(nodeSet);

        // Print the unique elements
        System.out.println("nodeList");
        for (Node2 node2 : nodeList) {
            System.out.println(node2);
        }
        
        // Create a new Set from the List (this removes duplicates)
        Set<Node2> uniqueNodeSet = new HashSet<>(nodeList);

        // Print the unique elements
        System.out.println("uniqueNodeSet");
        for (Node2 node2 : uniqueNodeSet) {
        	System.out.println(node2);
        }
		 */

	}

}


