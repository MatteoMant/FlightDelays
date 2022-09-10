package it.polito.tdp.extflightdelays.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {
	private Graph<Airport,DefaultWeightedEdge> grafo;
	private ExtFlightDelaysDAO dao;
	private Map<Integer,Airport> idMap;
	
	public Model() {
		dao = new ExtFlightDelaysDAO();
		idMap = new HashMap<Integer,Airport>();
		dao.loadAllAirports(idMap);
	}
	
	/**
	 * metodo che verrà richiamato ogni volta che l'utente clicca sul bottone 'Analizza aereoporti'.
	 * In questo modo il grafo viene sempre ricreato da zero
	 * @param x
	 */
	public void creaGrafo(int x) { // x = numero di compagnie minimo per filtrare gli aereoporti
		grafo = new SimpleWeightedGraph<Airport,DefaultWeightedEdge>(DefaultWeightedEdge.class);
		
		// aggiungere i vertici (NON tutti ma andremo ad aggiungere un sottoinsieme)
		Graphs.addAllVertices(this.grafo, dao.getVertici(x, idMap));
		
		// aggiungere gli archi
		for (Rotta r : dao.getRotte(idMap)) {
			if(this.grafo.containsVertex(r.getA1()) 
					&& this.grafo.containsVertex(r.getA2())) {
				DefaultWeightedEdge edge = this.grafo.getEdge(r.getA1(),r.getA2());
				if(edge == null) {
					Graphs.addEdgeWithVertices(this.grafo, r.getA1(), r.getA2(), r.getnVoli());
				} else {
					double pesoVecchio = this.grafo.getEdgeWeight(edge);
					double pesoNuovo = pesoVecchio + r.getnVoli();
					this.grafo.setEdgeWeight(edge, pesoNuovo);
				}
			}
		}
	}
	
	public int nVertici() {
		return this.grafo.vertexSet().size();
	}
	
	public int nArchi() {
		return this.grafo.edgeSet().size();
	}
	
	public List<Airport> getVertici(){
		List<Airport> vertici = new ArrayList<>(this.grafo.vertexSet());
		Collections.sort(vertici);
		return vertici;
	}
	
	// Per sapere a quali vertici è collegato un certo vertice di partenza possiamo usare il metodo 
	// connectedSetof(Vertex partenza) il quale ci restituisce tutti i vertici che possono essere raggiunti da 'partenza'.
	// Se il vertice di arrivo è presente all'interno di questo Set allora vorrà dire che esso può essere
	// raggiunto dal vertice di partenza e quindi che sono collegati. Un altro modo può essere quello di visitare
	// il grafo con un algortimo di visita e se incontriamo il nodo di destinazione allora sono connessi. 
	// In questo caso però noi siamo interessati proprio a calcolare il percorso: non ci basta visitare il grafo ma 
	// vogliamo ottenere un percorso dal vertice di partenza al vertice di arrivo. Pertanto dobbiamo recuperarci
	// l'albero di visita e, da questo, recuperarci il percorso.
	public List<Airport> getPercorso (Airport a1, Airport a2){
		 List<Airport> percorso = new ArrayList<>();
		 BreadthFirstIterator<Airport,DefaultWeightedEdge> it =
			 new BreadthFirstIterator<Airport,DefaultWeightedEdge>(this.grafo, a1);
		 
		 // L'idea è di partire dal vertice a1 e iniziare a visitare il grafo e, sfruttando il metodo getParent(), 
		 // possiamo poi risalire dalla destinazione alla sorgente, ottenendo così il percorso cercato.
		 
		 Boolean trovato = false;  // utilizziamo questa variabile sentinella per evitare di visitare tutto il grafo 
		 						   // ma possiamo fermarci non appena troviamo il nodo destinazione	 
		 // visito il grafo
		 while(it.hasNext() && trovato == false) {
			 Airport visitato = it.next(); // next() ci ritorna il vertice che l'iteratore ha appena visitato
			 if(visitato.equals(a2)) 
				 trovato = true; // ho trovato il nodo di destinazione
		 }
		 
		 // IMPORTANTE: prima di recuperare il percorso dobbiamo verificare che i due aereoporti siano 
		 // effettivamente collegati (quindi se 'trovato' == true)
		 
		 // ottengo il percorso : dobbiamo partire dal fondo per risalire fino alla radice (vertice sorgente)
		 if(trovato) { // cioè se i due aereoporti sono collegati
			 percorso.add(a2); // aggiungo per prima cosa la destinazione al percorso e da essa risalgo
			 Airport step = it.getParent(a2); // risalgo dalla destinazione a 'suo padre'
			 while (!step.equals(a1)) {  // risalgo sempre più in alto finchè non arrivo alla sorgente
				 percorso.add(0,step);  // aggiungo sempre in testa i vari vertici che visito in modo 
				 step = it.getParent(step);	// che la destinazione rimanga l'ultima
			 }
			 
			 percorso.add(0,a1); // aggiungo infine la sorgente in testa al percorso
			 return percorso;
		 } else {
			 return null; // in questo caso non esiste un percorso perchè il nodo destinazione NON appartiene 
		 }				  // all'insieme dei vertici raggiungibili dalla sorgente (i 2 aereoporti NON sono collegati)
	}
}