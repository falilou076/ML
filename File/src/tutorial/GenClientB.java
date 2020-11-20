package tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import umontreal.ssj.randvar.NormalGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

/**
 * GenClientB permet de g�n�rer les clients de type B
 * On cr�e un dictionnaire cl� valeur
 * La cl� repr�sente la p�riode
 * La valeur repr�sente la liste des conseillers qui contient chacune les rendez-vous des conseillers pour chaque plage
 * Pour un conseiller, sa liste constient les quatres rendez-vous pour chaque plage
 * Chaque rendez-vous est compos�e de l'heure d'arriv� du client et de son heure d'arriv� (heure arriv� + retard)
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
 
 	//On d�finit la loi qui g�n�re les arriv�s pour les clients de type B
  	NormalGen genArrB;
  	
	public GenClientB(){
		//On initialise la loi
		genArrB = new NormalGen(streamB, 100,90);
	}
	
	/**
	 * genInit donne l'heure pour laquelle une p�riode d�marre
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
	 * genClientB est la m�thode qui g�n�re les cleints de type B
	 * @param nbre_conseiller
	 * @return
	 */
	public HashMap genClientB(int nbre_conseiller[]){
		//On d�finit le dictionnare des rendez-vous
		HashMap dict_period_rv = new HashMap<String,LinkedList>();
		//On d�finit le nombre de p�riode
		int period = 3;
		for (int i =0; i<period ; i++) { //Pour chaque p�riode
			//On recupere la liste des conseillers avec leurs rendez-vous
			LinkedList rdv_cons = new LinkedList();
			
				int heure_rdv = genInit(i); //On initialise le d�but  de la p�riode 
				int conseiller = nbre_conseiller[i]; //On r�cup�re le nombre de conseiller pour la p�riode j
				
				for(int j=0; j<conseiller; j++) {
					//Pour chaque conseiller dans une p�riode j donn�e
					LinkedList rdv_plage = new LinkedList();
					for(int k=0; k<4;k++) { //Pour chaque plage
						ArrayList rdv_j_k = new ArrayList(); //Stocke les rendez-vous pour une p�riode j est chaque plage
						double heure_arr = heure_rdv + genArrB.nextDouble(); //On g�n�re l'heure d'arriv�: heure rendez-vous + heure g�n�r�e
						rdv_j_k.add(heure_rdv); //On ajoute heure de rendez-vous
						rdv_j_k.add(heure_arr); // On ajoute heure d'arriv�
						
						rdv_plage.addLast(rdv_j_k); //On l'ajoute dans la 
						
						heure_rdv += 30*MINUTE; // On incr�mente l'heure de rendez-vous de 30 minutes
					}
					rdv_cons.addLast(rdv_plage);
					heure_rdv = genInit(i);
				}
				
			//}
			//On ajoute l'ensemble des donn�es de la p�riode dans le dictionnaire avec la p�riode
			dict_period_rv.put(i, rdv_cons );
		}
		
		//On retourne le dictionnaire
		return dict_period_rv;
		
		
	}

}
