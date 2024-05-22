package core;
import java.io.*;
import java.util.*;

import de.texttech.cc.*;
import te.indexer.*;
import te.utils.*;
import JLanI.kernel.DataSourceException;
import JLanI.kernel.LanIKernel;
import JLanI.kernel.Request;
import JLanI.kernel.RequestException;
import JLanI.kernel.Response;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

//-Xms128m -Xmx1500m -Xverify:none

public class TextProcessing {

	String baseDocDir = "";
	String inputDirPath = "";
	String satzDirPath = "";
	static String outputDirPath = "";
	String indexesDirPath = "";

	int language = 0; // 1 for English 0 for German

	List ranked;

	// Resources for baseform reduction

	String redbase_en = "./resources/trees/en-nouns.tree";
	String redbase_de = "./resources/trees/de-nouns.tree";

	// Resources for compound splitting

	String red = "./resources/trees/grfExt.tree";

	String forw = "./resources/trees/kompVVic.tree";

	String back = "./resources/trees/kompVHic.tree";

	// Tagger Modelle
	String tmFile = "./resources/taggermodels/deTaggerModel.model";
	String tmFile2 = "./resources/taggermodels/english.model";

	public TextProcessing(String basedocdir) {

		baseDocDir = basedocdir;

		inputDirPath = baseDocDir + "/input/";
		satzDirPath = baseDocDir + "/satzfiles/";
		// satzFilePath = baseDocDir + "/satzfiles/satz.s";
		outputDirPath = baseDocDir + "/output/";
		indexesDirPath = baseDocDir + "/indexes/";

		File outputDir = new File(outputDirPath);
		cleanDir(outputDir);

		LanIKernel.propertyFile = "config/lanikernel";
	}

	public void preprocessing(int mode) {

		convertFiles(); // Convert input file formats into txt

		if (mode == 0) {
			extractSentenceFiles();
		} else if (mode == 1) {

			extractSentenceFileCorpus();

		}

	}

	public void convertFiles() {

		File inDir = new File(inputDirPath);

		if (inDir.isDirectory()) {

			File[] files = inDir.listFiles();

			Arrays.sort(files);

			// create subdirectory to contain the converted txt files:
			File txtDir = new File(inDir.getAbsolutePath(), "txt");
			if (!txtDir.exists()) {
				try {
					txtDir.mkdir();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else
				cleanDir(txtDir);

			// now loop through them and convert them
			for (int i = 0; i < files.length; i++) {

				File curFile = new File(files[i].getAbsolutePath());

				if (curFile.isFile()) {

					try {
						System.out.println("Convert " + curFile.getAbsolutePath());

						String curtext = "";
						InputStream inputStream = null;

						try {

							Parser parser = new AutoDetectParser();
							ContentHandler contentHandler = new BodyContentHandler(10 * 1024 * 1024);
							Metadata metadata = new Metadata();

							inputStream = new FileInputStream(curFile);

							parser.parse(inputStream, contentHandler, metadata, new ParseContext());

							/*
							 * for (String name : metadata.names()) { String value = metadata.get(name);
							 * System.out.println("Metadata Name: " + name);
							 * System.out.println("Metadata Value: " + value); }
							 * 
							 * System.out.println("Title: " + metadata.get("title"));
							 * System.out.println("Author: " + metadata.get("Author"));
							 */
							System.out.println("content: " + contentHandler.toString());
							curtext = contentHandler.toString();

						} catch (IOException e) {
							e.printStackTrace();
						} catch (TikaException e) {
							e.printStackTrace();
						} catch (SAXException e) {
							e.printStackTrace();
						} finally {
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}

						File destFile = new File(txtDir, curFile.getName() + ".txt");

						FileWriter f = new FileWriter(destFile);
						if (curtext != null) {
							f.write(curtext);
						}

						f.close();

					} catch (Exception e) {
						e.printStackTrace();
					}

				} else {
					// System.out.println("It is no file!");
				}
			} // for all files

		}

	}

	public void extractSentenceFiles() {

		File satzDir = new File(satzDirPath);

		cleanDir(satzDir);

		File inDir = new File(inputDirPath + "txt");

		if (inDir.isDirectory()) {

			File[] files = inDir.listFiles();

			Arrays.sort(files);

			// now loop through them
			for (int i = 0; i < files.length; i++) {

				File curFile = new File(files[i].getAbsolutePath());

				if (curFile.isFile()) {

					int language = getLanguage(curFile);

					String[] cmdArray = new String[5];

					if (language == 0)
						cmdArray[0] = "-L de";

					if (language == 1)
						cmdArray[0] = "-L en";

					cmdArray[1] = "-d" + curFile.getName(); // cmdArray[1] = "-dsatz"+(i+1);
					cmdArray[2] = "-p./" + satzDirPath; // /data/satzfiles";

					cmdArray[3] = "-a./resources/abbreviation/abbrev.txt";
					cmdArray[4] = curFile.getAbsolutePath();

					Text2Satz.main(cmdArray);

				} else {
					// System.out.println("It is no file!");
				}
			} // for all files

		}

	}

	public void extractSentenceFileCorpus() {

		File satzDir = new File(satzDirPath);

		cleanDir(satzDir);

		File inDir = new File(inputDirPath + "txt");

		if (inDir.isDirectory()) {

			File[] files = inDir.listFiles();

			String[] cmdArray = new String[4 + files.length];
			// cmdArray[0] = "-L de";
			cmdArray[1] = "-dcorpus"; // cmdArray[1] = "-dsatz";
			cmdArray[2] = "-p./" + satzDirPath; // data/satzfiles";

			cmdArray[3] = "-a./resources/abbreviation/abbrev.txt";

			for (int i = 0; i < files.length; i++) {

				File curFile = new File(files[i].getAbsolutePath());

				if (curFile.isFile()) {

					if (i == 0) {

						int language = getLanguage(curFile);

						if (language == 0)
							cmdArray[0] = "-L de";

						if (language == 1)
							cmdArray[0] = "-L en";

					}

					cmdArray[i + 4] = files[i].getAbsolutePath();

				} else {
					// System.out.println("It is no file!");
				}

			}

			if (files.length > 0)
				Text2Satz.main(cmdArray);

		}

	}

	int findEntry(List termlist, String query) {

		// int index = Collections.binarySearch(termlist, query);

		int index = -1;

		for (int i = 0; i < termlist.size(); i++)
			if (termlist.get(i).equals(query))
				index = i;

		return index;

	}

	private HashMap sortByValuesDec(HashMap map) {
		List list = new LinkedList(map.entrySet());

		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return -((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		HashMap sortedHashMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedHashMap.put(entry.getKey(), entry.getValue());
		}
		return sortedHashMap;
	}

	private HashMap sortByValuesInc(HashMap map) {
		List list = new LinkedList(map.entrySet());

		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		HashMap sortedHashMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedHashMap.put(entry.getKey(), entry.getValue());
		}
		return sortedHashMap;
	}

	public void createDB(boolean linkage) {

		File satzDir = new File(satzDirPath);

		if (satzDir.isDirectory()) {

			File[] files = satzDir.listFiles();

			for (int i = 0; i < files.length; i++) {

				File curFile = new File(files[i].getAbsolutePath());

				if (curFile.isFile()) {

					System.out.println("Filling DB with: " + curFile.getAbsolutePath());
					System.out.println("");
					
					if (!linkage) {
						Cooccs mycooccs = new Cooccs(curFile, true);
					}
					else {
						Linkage mylinkage = new Linkage(curFile);
					}

				}
			}
		}
	}

	public void analysis(boolean SCC) {

		File satzDir = new File(satzDirPath);

		if (satzDir.isDirectory()) {

			File[] files = satzDir.listFiles();

			Arrays.sort(files);

			for (int i = 0; i < files.length; i++) {

				File curFile = new File(files[i].getAbsolutePath());

				if (curFile.isFile()) {

					System.out.println("Analysing: " + curFile.getAbsolutePath());
					System.out.println("");

					Cooccs mycooccs = new Cooccs(curFile, false);

					float[][] cooccmatrix;
					List termlist = new Vector();

					System.out.println("Filling termlist...");
					Map cooccsmap = mycooccs.getCooccMap();

					// Liste aller Terme f�llen (dient als Lookup von Pos zu String)
					Set keys = cooccsmap.keySet();
					for (Iterator j = keys.iterator(); j.hasNext();) {
						String curStr = (String) j.next();

						termlist.add(curStr);
						writeLinesToFile(outputDirPath + "" + curFile.getName() + "_nodes_and_counts.txt",
								new String[] { "" + curStr }, true);

					}

					System.out.println("Number of all terms (types): " + termlist.size());
					writeLinesToFile(outputDirPath + "" + curFile.getName() + "_nodes_and_counts.txt",
							new String[] { "Number of all terms (types): " + termlist.size() }, true);

					cooccmatrix = new float[termlist.size()][termlist.size()];
					int edges = 0;
					// fill co-occurrence matrix

					try {
						System.out.println("Filling co-occurrence matrix...");

						Set coocckeys = cooccsmap.keySet();
						for (Iterator j = keys.iterator(); j.hasNext();) {
							String curStr = (String) j.next();

							int keyindex = findEntry(termlist, curStr);

							Map termCooccs = (Map) cooccsmap.get(curStr);

							Set cooccvalues = termCooccs.keySet();

							for (Iterator k = cooccvalues.iterator(); k.hasNext();) {

								String curStr2 = (String) k.next();
								float curSig = ((Float) termCooccs.get(curStr2)).floatValue();

								int keyindex2 = findEntry(termlist, curStr2);

								if ((keyindex != -1) && (keyindex2 != -1) /* && (keyindex!=keyindex2) */) {

									if (keyindex == keyindex2) {
										curSig = 1;

										cooccmatrix[keyindex][keyindex2] = curSig;
										edges++;

										// System.out.println(keyindex + " " +keyindex2 +" " + curSig) ;

									} else {

										if (curSig > 0) {

											cooccmatrix[keyindex][keyindex2] = curSig;
											// cooccmatrix[keyindex2][keyindex] = curSig;
											edges++;
											// System.out.println(keyindex + " " +keyindex2 +" " + curSig) ;

										} else {

											cooccmatrix[keyindex][keyindex2] = (float) 0.00; // 0.01
											// System.out.println("0" +keyindex + " " +keyindex2 +" " + curSig) ;

										}

									}

								}
							}

						} // cooccmatrix f�llen

						System.out.println("Number of all edges: " + edges );
						writeLinesToFile(outputDirPath + "" + curFile.getName() + "_nodes_and_counts.txt",
								new String[] { "Number of all edges: " + edges }, true);

						/************ ALGORITHMS ***************************/

						System.out.println("\nRunning Extended PageRank...");

						List pageranks = calculatePageRanks(termlist, cooccmatrix);

						int maxentries = 1000000; // 200;
						if (pageranks.size() < maxentries)
							maxentries = pageranks.size();

						float[][] simplifiedcooccmatrix = new float[maxentries][maxentries];
						String[] simplifiedtermlistvec = new String[maxentries];

						Vector centroidquery = new Vector();
						
						//boolean SCC = true; //moved to the parameter section
						
						if (SCC) {
					        String fileName = baseDocDir + "/Nodes.txt";
					        List<String> wordList = readWordsFromFile(fileName);
					        int counter = 0;
					        
							for (int j = 0; j < maxentries; j++) {

								Word curWord = (Word) pageranks.get(j);

								System.out.println("PageRank of " + curWord.getWordStr() + ": " + curWord.getSig());

								writeLinesToFile(outputDirPath + "" + curFile.getName() + "_termvector.txt",
										new String[] { "" + curWord.getWordStr() }, true);

								simplifiedtermlistvec[j] = curWord.getWordStr();

								// add List of words in the SCC
								
								// if not on j but on centroidquery size?
								if (counter < 5 && wordList.contains(curWord.getWordStr())) {
									System.out.println("---Word found in Nodelist!---");
									counter++;
									centroidquery.add(curWord.getWordStr());
								}
									
							}
							
						}
						else {
							for (int j = 0; j < maxentries; j++) {

								Word curWord = (Word) pageranks.get(j);

								System.out.println("PageRank of " + curWord.getWordStr() + ": " + curWord.getSig());

								writeLinesToFile(outputDirPath + "" + curFile.getName() + "_termvector.txt",
										new String[] { "" + curWord.getWordStr() }, true);

								simplifiedtermlistvec[j] = curWord.getWordStr();

								if (j < 5)
									centroidquery.add(curWord.getWordStr());
							}
						}



						
						
						
						System.out.println("\nRunning Centroid Calculation...");

						System.out.println("Determining centroid of document using query: " + centroidquery.toString());

						// if allowcentroidcandidatesinquery=false: do NOT allow query terms in list of
						// centroid candidates
						
						HashMap result;
						
						if (SCC) {
							result = mycooccs.getCentroidbySpreadingActivation(centroidquery, true);
						}
						else {
							result = mycooccs.getCentroidbySpreadingActivation(centroidquery, false);
						}
						
						String centroid = result.get("centroid").toString();

						System.out.println("Centroid of file '" + curFile.getName() + "' is: " + result.get("centroid"));
						writeLinesToFile(outputDirPath + "centroidsdata_" + curFile.getName() + ".txt",
								new String[] { "" + curFile.getName() + ";" + result.get("centroid") + ";"
										+ result.get("shortestaveragepathlength") }, true);

						System.out.println("Centroid candidate data: " + result.get("centroidcandidatesdata"));

						HashMap centroidcandidatesdata = (HashMap) result.get("centroidcandidatesdata");

						HashMap centroidcandidatesdatasorted = sortByValuesInc(centroidcandidatesdata);

						System.out.println("Centroid candidate data sorted: " + centroidcandidatesdatasorted);

						
						
						
						
						System.out.println("\nRunning Extended HITS...");

						List[] hits = calculateHITS(termlist, cooccmatrix);

						for (int j = 0; j < hits[0].size(); j++) {

							Word curWordauth = (Word) hits[0].get(j);
							String entry = curWordauth.getWordStr();
							String tag = "";
							int pos = curWordauth.getWordStr().indexOf("|");
							if (pos != -1) {
								entry = curWordauth.getWordStr().substring(0, pos);

								tag = curWordauth.getWordStr().substring(pos + 1, curWordauth.getWordStr().length());

							}

							System.out.println("Authority score of " + entry + ": " + curWordauth.getSig());

							writeLinesToFile(outputDirPath + "" + curFile.getName() + "_HITS.txt",
									new String[] { "" + entry + ";" + tag + ";" + curWordauth.getSig() + ";A" }, true);

						}

						for (int j = 0; j < hits[1].size(); j++) {

							Word curWordhubs = (Word) hits[1].get(j);
							String entry = curWordhubs.getWordStr();
							String tag = "";
							int pos = curWordhubs.getWordStr().indexOf("|");
							if (pos != -1) {
								entry = curWordhubs.getWordStr().substring(0, pos);
								tag = curWordhubs.getWordStr().substring(pos + 1, curWordhubs.getWordStr().length());
							}

							System.out.println("Hub score of " + entry + ": " + curWordhubs.getSig());

							writeLinesToFile(outputDirPath + "" + curFile.getName() + "_HITS.txt",
									new String[] { "" + entry + ";" + tag + ";" + curWordhubs.getSig() + ";H" }, true);

						}

					} catch (Exception e) {
						System.out.println("ERROR !: " + e);
						e.printStackTrace();
					}

				} else {
					System.out.println("It is no file!");
				}
			} // for all files

		}

	}

	// retrieves the list of a documents's most important terms (at most maxentries
	// terms)
	List getTermList(String document, int maxentries) {

		List termlist = new Vector();

		try {

			File curFile = new File(outputDirPath + "" + document);
			String line;

			int count = 0;

			FileInputStream fin = new FileInputStream(curFile);
			BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));

			while ((line = myInput.readLine()) != null) {
				termlist.add(line);
				count++;

				if (count >= maxentries)
					break;
			}

			myInput.close();
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return termlist;
	}
	
	//get entries from a list of strings, formatted like |String|number|
    private static List<String> readWordsFromFile(String fileName) {
        List<String> wordList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    String word = parts[1].trim();
                    wordList.add(word);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wordList;
    }

	// calculates the similarity between two documents (comparison of their term
	// vectors based on Dice coefficient)
	public double docsim(int doc1, int doc2) {

		String doc1name = "";
		String doc2name = "";

		double sim = -1.0;

		List docnames = new Vector();

		File outDir = new File(outputDirPath);

		if (outDir.isDirectory()) {

			File[] files = outDir.listFiles();

			Arrays.sort(files);

			for (int i = 0; i < files.length; i++) {

				File curFile = new File(files[i].getAbsolutePath());

				if (curFile.isFile()) {
					String docname = curFile.getName();
					if (docname.contains("termvector")) {
						docnames.add(docname);
					}
				}

			}

		}

		if (((docnames.size() - 1) >= doc1) && ((docnames.size() - 1) >= doc2)) {

			for (int i = 0; i < docnames.size(); i++) {

				if (i == doc1) {
					doc1name = (String) docnames.get(i);
				}

				if (i == doc2) {
					doc2name = (String) docnames.get(i);
				}

			}
		}

		if ((!doc1name.equals("")) && (!doc2name.equals(""))) {

			System.out.println("Comparing the following two texts: ");

			System.out.println(doc1name);
			System.out.println(doc2name);

			List doc1terms = getTermList(doc1name, 20);
			List doc2terms = getTermList(doc2name, 20);

			System.out.println("Termvector of text 1: " + doc1terms);
			System.out.println("Termvector of text 2: " + doc2terms);

			double commonterms = 0;

			if (doc1terms.size() == 0 || doc2terms.size() == 0)
				return sim;

			// check vectors' length and iterate over the shorter one
			if (doc1terms.size() > doc2terms.size()) {
				List temp = doc1terms;
				doc1terms = doc2terms;
				doc2terms = temp;
			}

			for (int i = 0; i < doc1terms.size(); i++) {
				String curTerm = (String) doc1terms.get(i);

				for (int j = 0; j < doc2terms.size(); j++) {

					String curTerm2 = (String) doc2terms.get(j);

					if (curTerm2.equals(curTerm)) {
						commonterms++;

					}

				}
			}

			sim = ((2 * commonterms) / (doc1terms.size() + doc2terms.size()));
		}

		return sim;
	}

	// Pagerank calculation
	List calculatePageRanks(List termlist, float[][] curgraph) {

		List pageranks_list = new Vector();

		float[] pagerank = new float[termlist.size()];
		float d = 0.85f;

		for (int i = 0; i < termlist.size(); i++) {
			pagerank[i] = (1 - d);
		}

		float[] out = new float[termlist.size()];

		for (int i = 0; i < termlist.size(); i++) {
			out[i] = 0;

			for (int j = 0; j < termlist.size(); j++) {

				if (curgraph[i][j] != 0)
					out[i] = out[i] + 1; // curgraph[i][j]; //+1

			}

		}

		for (int sz = 0; sz < 25; sz++) {

			for (int j = 0; j < termlist.size(); ++j) {

				float prj = 0;

				for (int i = 0; i < termlist.size(); i++) {

					if (curgraph[i][j] != 0) {

						prj = prj + (((pagerank[i] * curgraph[i][j]) / out[i]));

					}

				}

				pagerank[j] = (1 - d) + d * prj;

			}
			;

		}
		; // for sz

		float sum = 0;

		float maxpr = 0;
		int maxi = 0;

		for (int i = 0; i < termlist.size(); i++) {
			if (pagerank[i] > maxpr) {
				maxpr = pagerank[i];
				maxi = i;
			}

			sum = sum + pagerank[i];
		}

		for (int i = 0; i < termlist.size(); i++) {

			float value = 0;
			value = pagerank[i] / pagerank[maxi];

			Word curWord = new Word((String) termlist.get(i), 0);
			curWord.setSig(value);

			pageranks_list.add(curWord);

		}

		Collections.sort(pageranks_list);

		return pageranks_list;

	}

	// HITS calculation
	List[] calculateHITS(List termlist, float[][] curgraph) {

		List hits_hubs_list = new Vector();
		List hits_auths_list = new Vector();
		float[] hits_hubs = new float[termlist.size()];
		float[] hits_auths = new float[termlist.size()];

		for (int i = 0; i < termlist.size(); i++) {
			hits_auths[i] = 0.15f;
			hits_hubs[i] = 0.15f;
		}

		// Main loop
		for (int step = 0; step < 50; step++) {
			float norm = 0;

			// Authorities
			for (int j = 0; j < termlist.size(); ++j) {

				for (int i = 0; i < termlist.size(); i++) {

					if (curgraph[i][j] != 0) {
						hits_auths[j] = hits_auths[j] + (hits_hubs[i] * curgraph[i][j]);

					}

				}

				norm = norm + (hits_auths[j] * hits_auths[j]);

			}
			;

			norm = ((Double) Math.sqrt(norm)).floatValue();

			for (int j = 0; j < termlist.size(); ++j) {
				hits_auths[j] = hits_auths[j] / norm;

			}

			norm = 0;

			// Hubs
			for (int j = 0; j < termlist.size(); ++j) {

				for (int i = 0; i < termlist.size(); i++) {

					if (curgraph[j][i] != 0) {

						hits_hubs[j] = hits_hubs[j] + (hits_auths[i] * curgraph[j][i]);

					}

				}

				norm = norm + (hits_hubs[j] * hits_hubs[j]);

			}
			;

			norm = ((Double) Math.sqrt(norm)).floatValue();

			for (int j = 0; j < termlist.size(); ++j) {
				hits_hubs[j] = hits_hubs[j] / norm;

			}

		}
		; // for sz

		for (int i = 0; i < termlist.size(); i++) {

			float value = 0;

			value = hits_auths[i];

			Word curWord = new Word((String) termlist.get(i), 0);
			curWord.setSig(value);

			hits_auths_list.add(curWord);

		}

		for (int i = 0; i < termlist.size(); i++) {

			float value = 0;
			value = hits_hubs[i];

			Word curWord = new Word((String) termlist.get(i), 0);
			curWord.setSig(value);

			hits_hubs_list.add(curWord);

		}

		Collections.sort(hits_auths_list);
		Collections.sort(hits_hubs_list);

		List[] returnlist = new List[2];
		returnlist[0] = hits_auths_list;
		returnlist[1] = hits_hubs_list;

		return returnlist;
	}

	public int getLanguage(File satzFile) {

		String alltext = "";
		String lineorig;

		try {
			FileInputStream fin = new FileInputStream(satzFile);

			BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));

			while ((lineorig = myInput.readLine()) != null) {

				alltext = alltext + lineorig;

			}

			myInput.close();
			fin.close();
		} catch (Exception ex) {
		}

		// get the lanikernel-object
		LanIKernel lk = null;
		try {
			lk = LanIKernel.getInstance();
		} catch (DataSourceException e) {

		}

		// fill a request object
		Request req = null;
		try {
			// new request object
			req = new Request();
			// setting up the sentence/data
			req.setSentence(alltext);
			// dont collect useless information about the evaluation
			req.setReduce(true);
			// evaluate only log(datalength>=30) words for better speed
			req.setWordsToCheck(Math.max((int) Math.sqrt(alltext.length()), 30));
		} catch (RequestException e1) {

		}

		// evaluate the request
		Response res = null;
		try {
			// the evaluation call itself
			res = lk.evaluate(req);
		} catch (Exception e2) {

		}

		// search the winner language
		HashMap tempmap = res.getResult();
		double val = 0.0;
		String key = null, winner = "REST";
		for (Iterator iter = tempmap.keySet().iterator(); iter.hasNext();) {
			key = (String) iter.next();
			if (((Double) tempmap.get(key)).doubleValue() > val) {
				winner = key;
				val = ((Double) tempmap.get(key)).doubleValue();
			}
		}

		int languagevalue = 1;
		if (winner.equalsIgnoreCase("de")) {
			languagevalue = Parameters.DE;
			System.out.println("Language is DE\n");
		} else if (winner.equalsIgnoreCase("en")) {
			languagevalue = Parameters.EN;
			System.out.println("Language is EN\n");
		} else if (winner.equalsIgnoreCase("REST")) {

			languagevalue = Parameters.EN;
			System.out.println("Cannot determine language, taking EN\n");

		}

		return languagevalue;
	}

	private void cleanDir(File d) {

		if (d.isDirectory()) {
			File[] files = d.listFiles();
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
		}
	}

	public void writeLinesToFile(String filename, String[] linesToWrite, boolean appendToFile) {

		PrintWriter pw = null;

		try {

			if (appendToFile) {

				// If the file already exists, start writing at the end of it.
				pw = new PrintWriter(new FileWriter(filename, true));

			} else {

				pw = new PrintWriter(new FileWriter(filename));

			}

			for (int i = 0; i < linesToWrite.length; i++) {

				pw.println(linesToWrite[i]);

			}
			pw.flush();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			// Close the PrintWriter
			if (pw != null)
				pw.close();

		}
	}

	public static void main(String[] args) {

		TextProcessing textprocessing = new TextProcessing("data"); // base directory: data or download

		// Normally, run mode 1 first and then mode 0
		// Otherwise, centroid calculations will fail 
		// (as no underlying corpus graph database has been created yet)
		// use mode 2 instead of mode 1 for the creation of linkagegraphs

		int mode = 0; // mode=0 analyse single files, mode=1 analyse corpus 
					  // (to create local co-occurrence graph db)
		boolean SCC = false; // true: use only terms from nodelist for TRC calc, parameter for SCC graphs
							// false: use any term from the text for TRC calc

		if (mode == 0) { // Analyse single documents in data/input using  
			         	 // curr. Neo4j graph database created using mode 1

			long start = System.currentTimeMillis();

			textprocessing.preprocessing(mode);
			textprocessing.analysis(SCC);

			// Extra: Calculate document similarity if needed
			System.out.println("Similarity between document 1 and 2 is: " 
											+ textprocessing.docsim(0, 1));

			long end = System.currentTimeMillis();

			System.out.println("File processing took " + (end - start) / 1000 + " seconds.");

		} else if (mode == 1) { // Create Neo4j graph database only from all documents (corpus) in data/input

			long start = System.currentTimeMillis();

			textprocessing.preprocessing(mode);
			textprocessing.createDB(false); //false = cooccurencegraph, needs inputfiles 

			long end = System.currentTimeMillis();

			System.out.println("Graph creation took " + (end - start) / 1000 + " seconds.");

		} else if (mode == 2) { // Create Neo4j linkage graph database only from all documents (corpus) in data/input

			long start = System.currentTimeMillis();

			textprocessing.createDB(true); //true = linkagegraph, needs satzfiles, Words separated by |

			long end = System.currentTimeMillis();

			System.out.println("Processing took " + (end - start) / 1000 + " seconds.");

		}

	}

}
