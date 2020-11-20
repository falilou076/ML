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
	//ON d�finit les constantes pour les heures et les minutes
	static final double HOUR = 3600.0;  // Time is in seconds.
	static final double MINUTE = 60;
	/**
	 * <b>dict</b. est le dictionnare qui r�pertorie l'ensemble des rendez-vous <br>
	 * pour les clients de type B par p�riode et par conseillers (HashMap dict)  <br>
	 * On consid�re que chaque conseiller a un rendez-vous pour chaque plage et on g�n�re les clients. <br>
	 * On discutera suivant l'arriv� en utilsant la probabilit� de pr�sence <br>
	 */
	HashMap dict;
	double prob_seuil; //Probabilite pour que le client arrive
	int[] conseillers;
	LognormalGen genServB; //loi pour g�n�rer les services des clients de type B
	LinkedList temps_libre = new LinkedList<>(); //Liste qui contient l'ensemble des heures libres des conseillers
	RandomStream streamB        = new MRG32k3a(); 
	RandomStream streamArr      = new MRG32k3a(); // pour les temps d'arriv�s
	RandomStream streamPatience = new MRG32k3a(); // temps de patience
	   
	//Tally pour r�colter les statistiques pour les temps d'attente moyen par client
	TallyStore statWaits = new TallyStore ("Temps d'attente moyen par client");
	   
	   public BankBf() {
		   //On g�n�re les clients de type B
		   GenClientB clientBDict = new GenClientB();
		   int[] conseiller = {2,3,3}; //Initialiser directement
		   conseillers = conseiller;
		   prob_seuil = 0.05;
		   dict = clientBDict.genClientB(conseiller); //On g�n�re les cleints de type B
		   genServB = new LognormalGen(new MRG32k3a(),0.29,0.9); //Loi pour g�n�rer les services
	   }
	   
	   
	   /**
	    * 
	    * @author Mohamet Tall && Falilou Fall && Mame Diarra Sow <br>
	    *Service_B simule les clients B  <br>
	    *Les clients �tant d�ja g�n�rer initialement <br> 
	    *La simulation se d�roule comme suit: <br>
	    *<ul>
	    *<li>On parcours avec la boucle pour r�cup�rer la listes des conseillers pour chaque p�riode</li>
	    *<li>La liste obtenue montre les conseillers avec leurs rendez-vous</li>
	    *<li>On parcours � nouveau la liste obtenue pour r�cup�rer un conseiller qu'on met dans une nouvelle liste</li>
	    *<li>Avec cette, nous avons acc�s aux diff�rnets rendez-vous pour chaque conseiller</li>
	    *<li>Chaque rendez-vous contient l'heure de rendez-vous et  l'heure d'arriv� </li>
	    *<li>Pour chaque client, on g�n�re le temps de service et on on enregistre dans le Tally</li>
	    *<li>Un client B qui arrive est servi directement si le conseiller est libre
	    * sinon attends jusqu'� ce que le conseiller termine pour prendre son service</li>
	    *</ul>
	    */
	   class Service_B{
		   public Service_B(int period) {
			   int plage =4; //Nombre de plages
			   for(int i=0; i<period; i++) { //Pour chaque p�riode
				   LinkedList t = (LinkedList)dict.get(i); //listes conseillers avec leurs rendez-vous pour la  p�riode j 
				   for(int j=0; j<conseillers[i]; j++) { // Pour chaque conseiller
					   LinkedList emp_period = (LinkedList)t.get(0); //On recup�re la liste du conseiller avec les 4 rendez-vous pour chaque plage
					   LinkedList serviceTimeTab = new LinkedList<>();
					   //Generer temps de service pour chaque rendez vous pour chaque client
					   for(int k=0; k<plage; k++) {
						   serviceTimeTab.addLast(genServB.nextDouble());
					   }
					   //On parcours chaque plage
					   for(int l=0; l<plage; l++) {
						double p = Math.random(); //On g�n�re la probabilit� compris entre 0 et 1   
						  if(l<3) {
							  ArrayList emp_rvl = (ArrayList)emp_period.get(l);// On r�cup�re le client l 
							   ArrayList emp_rvl_1 = (ArrayList)emp_period.get(l+1); //On r�cup�re le client l+1
							   double arr_l = (double) emp_rvl.get(1); //On r�cup�re le temps d'arriv� du client l
							   double arr_l_1 =(double)emp_rvl_1.get(1); //On r�cup�re le temps d'arriv� du client l+1
							   int rdv_l = (int) emp_rvl.get(0); //On r�cup�re le rendez-vous du client l
							   double service_time_l = (double) serviceTimeTab.get(l)+ 30*MINUTE; //On r�cup�re le temps de service du client l
							   double service_time_l_1 = (double) serviceTimeTab.get(l+1); //On r�cup�re le temps de service du cleint l+1
							   /**
							    * Le temps de patience du client l+1 est �gale au temps d'arriv� du client l plus
							    * le temps de service du client l moins le temps d'arriv� du client l+1
							    */
							   double patience_l_1 = arr_l + service_time_l - arr_l_1;
							   
							  if(p> prob_seuil ) {//p sup�rieure � la probabil� seuil de pr�sence
								   if(patience_l_1 >= 0) { //Si le temps de patience calculer est postive
									   statWaits.add(patience_l_1);
								   }else { //Si le temps de patience calcul� est n�gatif
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
	    * Permet de simuler par p�riode <br>
	    * Elle returne la m�thode Service_B avec la p�riode en input <br>
	    * @param period
	    */
	   public void Simulate(int period) {
		   new Service_B(period);
	   }
	
	
   /**
    * startB() ex�cute les �tapes suivantes: <br>
    * <ul>
    * <li>Simule la banque pendant un nombre de jours donn� ici 1000 en consid�rant les p�riodes</li>
    * <li>Affiche le rapport de la simulation pour les cleints de type B de la banque </li>
    * <li>Trace l'histogramme en utilisant le collecteur statistique:
    * <ul>
    * <li>Temps d'attente moyen en abscisse</li>
    * <li>Nombre d'observation en ordonn�</li>
    * </ul>
    * </li>
    * </ul>
    */
	public void startB() {
		//Simuler pour 1000 jours en 3 p�riode
		for(int n=0; n<1000; n++) {
			Simulate(3);
		}
		//Affiche le rapport
		System.out.println("Bank B");
		System.out.println (statWaits.report());
		//On initialise l'histogramme avec les temps d'attente moyen en abscisse et le nombre d'observation en ordonn�es
		HistogramChart hist = new HistogramChart("Histogramme B ",
	    		  "temps d'attente moyen",
	    		  "Nombre d'observation",
	    		  ((TallyStore) statWaits).getArray(),
	    		  ((TallyStore) statWaits).numberObs()
	    	);
	      hist.view(800, 500);
	}
	
	

}
