package tutorial;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

import umontreal.ssj.charts.HistogramChart;
import umontreal.ssj.probdist.ExponentialDist;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.randvar.LognormalGen;
import umontreal.ssj.randvar.PoissonGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;


/**
 * 
 * @author Mohamet Tall && Falilou Fall && Mame Diarra Sow <br>
 * BankBf simule les clients de type B de la banque <br>
 *
 */
public class BankBf {
	//ON définit les constantes pour les heures et les minutes
	static final double HOUR = 3600.0;  // Time is in seconds.
	static final double MINUTE = 60;
	/**
	 * <b>dict</b. est le dictionnare qui répertorie l'ensemble des rendez-vous <br>
	 * pour les clients de type B par période et par conseillers (HashMap dict)  <br>
	 * On considére que chaque conseiller a un rendez-vous pour chaque plage et on génére les clients. <br>
	 * On discutera suivant l'arrivé en utilsant la probabilité de présence <br>
	 */
	HashMap dict;
	double prob_seuil; //Probabilite pour que le client arrive
	int[] conseillers;
	LognormalGen genServB; //loi pour générer les services des clients de type B
	LinkedList temps_libre = new LinkedList<>(); //Liste qui contient l'ensemble des heures libres des conseillers
	RandomStream streamB        = new MRG32k3a(); 
	RandomStream streamArr      = new MRG32k3a(); // pour les temps d'arrivés
	RandomStream streamPatience = new MRG32k3a(); // temps de patience
	   
	//Tally pour récolter les statistiques pour les temps d'attente moyen par client
	TallyStore statWaits = new TallyStore ("Temps d'attente moyen par client");
	   
	   public BankBf() {
		   //On génére les clients de type B
		   GenClientB clientBDict = new GenClientB();
		   int[] conseiller = {2,3,3}; //Initialiser directement
		   conseillers = conseiller;
		   prob_seuil = 0.05;
		   dict = clientBDict.genClientB(conseiller); //On génére les cleints de type B
		   genServB = new LognormalGen(new MRG32k3a(),0.29,0.9); //Loi pour générer les services
	   }
	   
	   
	   /**
	    * 
	    * @author Mohamet Tall && Falilou Fall && Mame Diarra Sow <br>
	    *Service_B simule les clients B  <br>
	    *Les clients étant déja générer initialement <br> 
	    *La simulation se déroule comme suit: <br>
	    *<ul>
	    *<li>On parcours avec la boucle pour récupérer la listes des conseillers pour chaque période</li>
	    *<li>La liste obtenue montre les conseillers avec leurs rendez-vous</li>
	    *<li>On parcours à nouveau la liste obtenue pour récupérer un conseiller qu'on met dans une nouvelle liste</li>
	    *<li>Avec cette, nous avons accés aux différnets rendez-vous pour chaque conseiller</li>
	    *<li>Chaque rendez-vous contient l'heure de rendez-vous et  l'heure d'arrivé </li>
	    *<li>Pour chaque client, on génére le temps de service et on on enregistre dans le Tally</li>
	    *<li>Un client B qui arrive est servi directement si le conseiller est libre
	    * sinon attends jusqu'à ce que le conseiller termine pour prendre son service</li>
	    *</ul>
	    */
	   class Service_B{
		   public Service_B(int period) {
			   int plage =4; //Nombre de plages
			   for(int i=0; i<period; i++) { //Pour chaque période
				   LinkedList t = (LinkedList)dict.get(i); //listes conseillers avec leurs rendez-vous pour la  période j 
				   for(int j=0; j<conseillers[i]; j++) { // Pour chaque conseiller
					   LinkedList emp_period = (LinkedList)t.get(0); //On recupére la liste du conseiller avec les 4 rendez-vous pour chaque plage
					   LinkedList serviceTimeTab = new LinkedList<>();
					   //Generer temps de service pour chaque rendez vous pour chaque client
					   for(int k=0; k<plage; k++) {
						   serviceTimeTab.addLast(genServB.nextDouble());
					   }
					   //On parcours chaque plage
					   for(int l=0; l<plage; l++) {
						double p = Math.random(); //On génére la probabilité compris entre 0 et 1   
						  if(l<3) {
							  ArrayList emp_rvl = (ArrayList)emp_period.get(l);// On récupére le client l 
							   ArrayList emp_rvl_1 = (ArrayList)emp_period.get(l+1); //On récupére le client l+1
							   double arr_l = (double) emp_rvl.get(1); //On récupére le temps d'arrivé du client l
							   double arr_l_1 =(double)emp_rvl_1.get(1); //On récupére le temps d'arrivé du client l+1
							   int rdv_l = (int) emp_rvl.get(0); //On récupére le rendez-vous du client l
							   double service_time_l = (double) serviceTimeTab.get(l)+ 30*MINUTE; //On récupére le temps de service du client l
							   double service_time_l_1 = (double) serviceTimeTab.get(l+1); //On récupére le temps de service du cleint l+1
							   /**
							    * Le temps de patience du client l+1 est égale au temps d'arrivé du client l plus
							    * le temps de service du client l moins le temps d'arrivé du client l+1
							    */
							   double patience_l_1 = arr_l + service_time_l - arr_l_1;
							   
							  if(p> prob_seuil ) {//p supérieure à la probabilé seuil de présence
								   if(patience_l_1 >= 0) { //Si le temps de patience calculer est postive
									   statWaits.add(patience_l_1);
								   }else { //Si le temps de patience calculé est négatif
									   statWaits.add(0.0);
								   }
							  }else { //i.e si le client ne vient pas
								  if(arr_l_1 - arr_l - service_time_l >= 0) {
									  //On enregistre le temps libre
									  temps_libre.addLast(arr_l_1 - arr_l - service_time_l);
								  }
							  }
							  
						  }
						  
					   }
				   }
			   }
			
		   }
	   }
	   
	   /**
	    * Permet de simuler par période <br>
	    * Elle returne la méthode Service_B avec la période en input <br>
	    * @param period
	    */
	   public void Simulate(int period) {
		   new Service_B(period);
	   }
	
	
   /**
    * startB() exécute les étapes suivantes: <br>
    * <ul>
    * <li>Simule la banque pendant un nombre de jours donné ici 1000 en considérant les périodes</li>
    * <li>Affiche le rapport de la simulation pour les cleints de type B de la banque </li>
    * <li>Trace l'histogramme en utilisant le collecteur statistique:
    * <ul>
    * <li>Temps d'attente moyen en abscisse</li>
    * <li>Nombre d'observation en ordonné</li>
    * </ul>
    * </li>
    * </ul>
    */
	public void startB() {
		//Simuler pour 1000 jours en 3 période
		for(int n=0; n<1000; n++) {
			Simulate(3);
		}
		//Affiche le rapport
		System.out.println("Bank B");
		System.out.println (statWaits.report());
		//On initialise l'histogramme avec les temps d'attente moyen en abscisse et le nombre d'observation en ordonnées
		HistogramChart hist = new HistogramChart("Histogramme B ",
	    		  "temps d'attente moyen",
	    		  "Nombre d'observation",
	    		  ((TallyStore) statWaits).getArray(),
	    		  ((TallyStore) statWaits).numberObs()
	    	);
	      hist.view(800, 500);
	}
	
	

}
