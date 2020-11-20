package tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import umontreal.ssj.randvar.NormalGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

/**
 * GenClientB permet de générer les clients de type B
 * On crée un dictionnaire clé valeur
 * La clé représente la période
 * La valeur représente la liste des conseillers qui contient chacune les rendez-vous des conseillers pour chaque plage
 * Pour un conseiller, sa liste constient les quatres rendez-vous pour chaque plage
 * Chaque rendez-vous est composée de l'heure d'arrivé du client et de son heure d'arrivé (heure arrivé + retard)
 * @author Mohamet Tall && Falilou Fall && Mame Diarra Sow 
 *
 */

public class GenClientB {
	//On initialise les constantes heures et minutes
	int HOUR = 3600;
	int MINUTE = 60;
	RandomStream streamB        = new MRG32k3a(); // For B.
  	RandomStream streamPatience = new MRG32k3a(); // For patience times.
  	RandomStream streamArrB    = new MRG32k3a(); // For arrivals.
 
 	//On définit la loi qui génére les arrivés pour les clients de type B
  	NormalGen genArrB;
  	
	public GenClientB(){
		//On initialise la loi
		genArrB = new NormalGen(streamB, 100,90);
	}
	
	/**
	 * genInit donne l'heure pour laquelle une période démarre
	 * 10, 12  ou 14 heures
	 * @param period
	 * @return 
	 */
	public int genInit(int period){
		if (period == 0) {
			return 10*HOUR;
		}
		if (period == 1) {
			return 12*HOUR;
		}
		
		return 14*HOUR;
	}
	
	/**
	 * genClientB est la méthode qui génére les cleints de type B
	 * @param nbre_conseiller
	 * @return
	 */
	public HashMap genClientB(int nbre_conseiller[]){
		//On définit le dictionnare des rendez-vous
		HashMap dict_period_rv = new HashMap<String,LinkedList>();
		//On définit le nombre de période
		int period = 3;
		for (int i =0; i<period ; i++) { //Pour chaque période
			//On recupere la liste des conseillers avec leurs rendez-vous
			LinkedList rdv_cons = new LinkedList();
			
				int heure_rdv = genInit(i); //On initialise le début  de la période 
				int conseiller = nbre_conseiller[i]; //On récupére le nombre de conseiller pour la période j
				
				for(int j=0; j<conseiller; j++) {
					//Pour chaque conseiller dans une période j donnée
					LinkedList rdv_plage = new LinkedList();
					for(int k=0; k<4;k++) { //Pour chaque plage
						ArrayList rdv_j_k = new ArrayList(); //Stocke les rendez-vous pour une période j est chaque plage
						double heure_arr = heure_rdv + genArrB.nextDouble(); //On génére l'heure d'arrivé: heure rendez-vous + heure générée
						rdv_j_k.add(heure_rdv); //On ajoute heure de rendez-vous
						rdv_j_k.add(heure_arr); // On ajoute heure d'arrivé
						
						rdv_plage.addLast(rdv_j_k); //On l'ajoute dans la 
						
						heure_rdv += 30*MINUTE; // On incrémente l'heure de rendez-vous de 30 minutes
					}
					rdv_cons.addLast(rdv_plage);
					heure_rdv = genInit(i);
				}
				
			//}
			//On ajoute l'ensemble des données de la période dans le dictionnaire avec la période
			dict_period_rv.put(i, rdv_cons );
		}
		
		//On retourne le dictionnaire
		return dict_period_rv;
		
		
	}

}
