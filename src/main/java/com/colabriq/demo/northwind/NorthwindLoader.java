package com.colabriq.demo.northwind;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.rdf.model.Model;

import com.colabriq.model.Link;
import com.colabriq.model.Link.RelType;
import com.colabriq.shared.Skolemizer;
import com.colabriq.shared.treesort.TreeNode;
import com.colabriq.shared.treesort.TreeSort;
import com.colabriq.utils.RDFClient;
import com.google.gson.JsonParser;

/**
 * Ingests a directory of files (sorting them first) and pushes them to the relevant endpoints of different actors.
 *
 */
public class NorthwindLoader {
	public static final RDFClient CLIENT;
	public static final String DEFAULT_ENDPOINT = "http://localhost:8081/";
	
	static {
		try {
			var endpoint = System.getenv("ENDPOINT");
			if (endpoint == null) {
				endpoint = DEFAULT_ENDPOINT;
			}
			
			CLIENT = new RDFClient(new URI(endpoint));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * A node of Northwind information ready to be containerized
	 */
	class NorthwindContainer implements TreeNode<String> {
		private Model model;
		private final String sub;
		private final Set<String> objs;
		
		public NorthwindContainer(Model model) {
			this.model = model;
			var iter = model.getGraph().find();
			
			var _subs = new HashSet<String>();
			var _objs = new HashSet<String>();
			
			while (iter.hasNext()) {
				var triple = iter.next();
			
				// collect up subjects (should be all the same!)
				if (triple.getSubject().isURI()) {
					_subs.add(triple.getSubject().getURI());
				}
				else {
					throw new RuntimeException("Don't know how to deal with " + triple.getSubject());
				}
				
				if (triple.getObject().isURI()) {
					var uri = triple.getObject().getURI();
					if (uri.startsWith("http://northwind.com/") &&
						!uri.startsWith("http://northwind.com/model/") && 
						!uri.startsWith("http://northwind.com/data/")) {
						
						_objs.add(uri);
					}
				}
			}
			
			if (_subs.size() == 1) {
				this.sub = _subs.iterator().next();
			}
			else {
				throw new RuntimeException("Multiple subjects: " + _subs);
			}
			
			this.objs = _objs;
		}

		@Override
		public String getValue() {
			return sub;
		}

		@Override
		public Stream<String> getPredecessors() {
			return objs.stream();
		}
	}

	private File file;
	
	protected NorthwindLoader(File file) {
		this.file = file;
	}
	
	public void run() throws Exception {
		List<String> prefixes = new LinkedList<>();
		List<List<String>> blocks = new LinkedList<>();
		List<String> block = new LinkedList<>();
		
		try (var reader = new BufferedReader(new FileReader(file))) {
			// read the file line by line
			
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue; // ignore comments
				}
				else if (line.startsWith("@prefix ")) {
					prefixes.add(line);
				}
				else if (line.trim().equals("")) {
					// end of block
					if (block.size() > 0) {
						blocks.add(block);
						block = new LinkedList<>();
					}
				}
				else {
					block.add(line);
				}
			}
		}
		
		var allPrefixes = StringUtils.join(prefixes, "\n");
		var containers = new LinkedList<NorthwindContainer>();
		
		// reassemble each block as a mini-graph.
		blocks.forEach(b -> {			
			String ttl =
				allPrefixes + "\n\n" + 
				StringUtils.join(b, "\n");
			
			// parse + load as Graph
			var dataset = Skolemizer.autoSkolemizingDataset();
			var model = dataset.getDefaultModel();
			model.read(new StringReader(ttl), null, "TTL");
			
			containers.add(new NorthwindContainer(model));
		});
		
		var sorted = TreeSort.sort(containers);
		var linkMap = new HashMap<String, String>();
		var jsonParser = new JsonParser();
		
		sorted.forEach(container -> {
			var writer = new StringWriter();

			// prepare chain of custody
			var links = container.objs.stream().map(obj ->
				new Link(
					Optional.ofNullable(linkMap.get(obj)).orElseThrow(() -> new RuntimeException("No linkMap for " + obj)), 
					RelType.CAUSED_BY
				)
			);
			
			// write out turtle
			container.model.write(writer, "TURTLE");
			
			var ttl = writer.toString();
			
			System.out.println(ttl);
			System.out.println("(length = " + ttl.length() + ")");
			
			var result = CLIENT.upload("next.ttl", "text/turtle", new StringReader(ttl), links);
			
			var jsonObject = jsonParser.parse(result).getAsJsonObject();
			
			// record ID against subject
			var id = jsonObject.get("inner_envelope").getAsJsonObject().get("hashkey").getAsString();
			linkMap.put(container.sub, id);
			
			System.out.println("----------------------------------------");
		});
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Must specify location of northwind.ttl");
			return;
		}
		
		var nwFile = new File(NorthwindLoader.class.getClassLoader().getResource(args[0]).getFile());
		new NorthwindLoader(nwFile).run();
	}
}
