package tutorial;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

import umontreal.ssj.probdist.ExponentialDist;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.randvar.GammaAcceptanceRejectionGen;
import umontreal.ssj.randvar.GammaGen;
import umontreal.ssj.randvar.LognormalGen;
import umontreal.ssj.randvar.PoissonGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.charts.HistogramChart;

/**
 * Bank_A est une classe qui simule les clients de type dans la banque <br>
 * Elle est composée de différentes classes à savoir <br>
 * <ul> 
 * <li>Service_A</li>
 * <li>NextPeriod qui étends la classe Event de ssj</li>
 * <li>Arrival_A qui gére les arrivés</li>
 * <li>ServiceCompletion pour le service complet</li>
 * </ul>
 * <br>
 * @author Mohamet Tall && Falilou Fall && Mame Diarra Sow
 *
 */

public class Bank_A {
	/**
	 * On définit les constantes HOUR et MINUTE <br>
	 */
   static final double HOUR = 3600.0;  // Time is in seconds.
   static final double MINUTE = 60.0;
   
   /**
    * Déclaration des variables <br>
    * La pluspart de ces variables sont lis dans le fichier <br>
    */

   //Taux d'arrivés par heures, services et temps de patience en secondes

   double openingTime;    // Heures d'ouvertures de la banque 
   int numPeriods;        // Nombre de période par jour
   int[] numCaissiers;    // Nombre d'agent pour chaque période
   double[] lambda;       //Taux d'arrivé pour lambda_j pour chaque serveur 
   double alpha0;         //Paramétre distribution gamma pour B
   double p;              // Probabilité pour que le client Arrive
   double nu;             // Paramétre temps de patience pour la loi exponentielle
   double alpha, beta;    //Parametre gamma temps de service
   double s;              // Temps de patience inferieure à s

   // Variables
   double busyness;      
   double arrRate = 0.0;  
   int nCaissiers;           // Nombre de caissiers pour chaque période
   int nBusy;             // Nombre d'agents occupés
   int nArrivals;         // Nombre arrivé par jour
   int nGoodQoS;          // Nombre qui ont un temps d'attente inférieur à s
   double nServiceE; 		// Nombre de service espéré par jour
   double sigma; //Parametres genService A
   //Params loi de poisson a calculer
   double nuA;
   double sigmaA; 
   double lambdaJ;
   double temps_libre_conseiller;
   Event nextArrival = new Arrival_A();           // The next Arrival event.
   LinkedList<Service_A> waitList = new LinkedList<Service_A>();

   //On déclare les random stream
   RandomStream streamB        = new MRG32k3a(); 
   RandomStream streamArr      = new MRG32k3a(); // Arrivé
   RandomStream streamPatience = new MRG32k3a(); // Temps de patience
   LognormalGen genServA;     
   PoissonGen genArrA;

   //Déclaration des collecteurs statistiques
   TallyStore[] allTal = new TallyStore [3];
   Tally statArrivals = allTal[0] = new TallyStore ("Number of arrivals per day");
   Tally statWaits = allTal[1] = new TallyStore ("Average waiting time per customer");
   Tally statWaitsDay = allTal[2] = new TallyStore ("Waiting times within a day");
   
   //Array qui contient les temps libre pour les conseillers
   LinkedList temps_libre_B ;
   

   public Bank_A (String fileName, LinkedList temps_libre) throws IOException {
      readData (fileName);
      sigma = sigma/60;
      nu = nu /60; 
      //Calcul de parametres pour la loi  LognormalGen
      sigmaA = Math.sqrt(Math.log(1 + ((sigma*sigma)/(nu*nu)))); //convertir en minutes
      nuA = (Math.log(nu) - (0.5*sigmaA*sigmaA)); //converti en minute

      genServA = new LognormalGen(new MRG32k3a(),nuA,sigmaA);
      //On initialise la liste pour les temps libres des conseillers
      temps_libre_B = temps_libre;
      
   }

   // Lecture des données
   public void readData (String fileName) throws IOException {
      Locale loc = Locale.getDefault();
      Locale.setDefault(Locale.US); // to read reals as 8.3 instead of 8,3
      BufferedReader input = new BufferedReader(new FileReader (fileName));
      Scanner scan = new Scanner(input);
      openingTime = scan.nextDouble();      scan.nextLine();
      numPeriods = scan.nextInt();          scan.nextLine();
      numCaissiers = new int[numPeriods];
      lambda = new double[numPeriods];
      nServiceE = 0.0;
      for (int j = 0; j < numPeriods; j++) {
         numCaissiers[j] = scan.nextInt();
         lambda[j] = scan.nextDouble();
         nServiceE += lambda[j];       scan.nextLine();
      }
      alpha0 = scan.nextDouble();      scan.nextLine();
      p = scan.nextDouble();           scan.nextLine();
      nu = scan.nextDouble();          scan.nextLine();
      alpha = scan.nextDouble();       scan.nextLine();
      beta = scan.nextDouble();        scan.nextLine();
      s = scan.nextDouble();           scan.nextLine();
      sigma = scan.nextDouble();
      scan.close();
      Locale.setDefault(loc);
   }

   /**
    * Service_A gére les services des clients de type A <br>
    * Le constructeur vérifie si le nombre de client occupés est inférieur au nombre de caissiers ou supérieur <br>
    * S'il est inférieur, on sert directrement le client <br>
    * Sinon, on le met dans la file d'attente <br>
    *Elle comprends aussi la méthode endWait() qui s'éxécute si l'attente est terminée <br>
    * 
    */
   class Service_A {
      double arrivalTime, serviceTime, patienceTime;

      public Service_A() {
         serviceTime = genServA.nextDouble(); // Generate service time.
         if (nBusy < nCaissiers) {           // Start service immediately.
            nBusy++;
            nGoodQoS++;
            statWaitsDay.add (0.0);
            new ServiceCompletionA().schedule (serviceTime);
         } else {                         // Join the queue.
            patienceTime = generPatience();
            arrivalTime = Sim.time();
            statWaitsDay.add(patienceTime);
            waitList.addLast (this);
         }
      }
      
     
      
      public void endWait() {
         double wait = Sim.time() - arrivalTime;
         nBusy++;
         new ServiceCompletionA().schedule (serviceTime);
         if (wait < s) nGoodQoS++;
         statWaitsDay.add (wait);
         
      }
   }

   /**
    * NextPeriod gére les évenements pour les nouvelles périodes <br>
    * @author Mohamet Tall && Falilou Fall && Mame Diarra Sow
    *
    */
   
   class NextPeriod extends Event {
      int j;     // Number of the new period.
      public NextPeriod (int period) { j = period; }
      public void actions() {
         if (j < numPeriods) {
            nCaissiers = numCaissiers[j]; //On recupere nCaissiers
            arrRate = busyness * lambda[j] / HOUR; //On calcule le taux d'arrivé
            lambdaJ = arrRate;
            genArrA = new PoissonGen(new MRG32k3a(), lambdaJ); //Loi pour générer les arrivés des clents de type A
            if (j == 0) {          
            	//On planifie une nouvelle arrivée
               nextArrival.schedule
                  (genArrA.nextDouble());
               	
            } else {
               checkQueue();

               //Cette méthode checkQueueB() permet aux conseillers de servir des clients de type A s'ils sont libre 
               checkQueueB();
               //On planifie une nouvelle arrivée
               nextArrival.reschedule ((nextArrival.time() - Sim.time())
                                       * lambda[j-1] / lambda[j]);
            }
            new NextPeriod(j+1).schedule (2.0 * HOUR); //Par 2 heures
         } else
            nextArrival.cancel();  // Fin de la journée
      }
   }
 
   // Evenement: un client arrive
   class Arrival_A extends Event {
      public void actions() {
         nextArrival.schedule
            (ExponentialDist.inverseF (arrRate, streamArr.nextDouble()));
         nArrivals++;
         new Service_A();               // Client arrivé
      }
   }

   // Evénement: Service complet
   class ServiceCompletionA extends Event {
      public void actions() { nBusy--;   checkQueue(); }
   }

   // Servir le client si l'agent est libre est que la file n'est pas vide 
   
   public void checkQueue() {
      while ((waitList.size() > 0) && (nBusy < nCaissiers))
         (waitList.removeFirst()).endWait();
   }
   
   //Client A servi par un conseiller
   /**
    * checkQueueB est une méthode qui permet de servir les clients de type A <br> 
    * si les conseillers qui devraient servir les clients de type B sont libres <br>
    * Elle regarde l'ensemble des temps libres pour les conseillers et sert les clients de type A <br>
    * en vérifie que le service n'a pas de rendez-vous durant au moins égal à s. <br>
    */
   public void checkQueueB() {
	   for(int i=0; i<temps_libre_B.size();i++) {
		   double temps = (double) temps_libre_B.getFirst();
		   double serviceTimeB = genServA.nextDouble();
		   while(waitList.size()>0 && temps > s && nBusy == nCaissiers) {
			   (waitList.removeFirst()).endWait();
			   temps -= serviceTimeB;
		   }
		   temps_libre_B.removeFirst();
	   }
   }

   /**
    * genererPatience permet de générer les patinces pour les cleints de type A <br>
    * @return la valeur générer en utilisant la loi ExponentialDist:inverse <br>
    */
   public double generPatience() {
      double u = Math.random(); //Pas abandon
      return ExponentialDist.inverseF (u, p);
   }

   /**
    * simulateOneDay permet de simuler un jour dans la  banque pour les cleints de type A <br>
    * @param busyness
    */
   public void simulateOneDay (double busyness) {
      Sim.init();        statWaitsDay.init();
      nArrivals = 0;     
      nGoodQoS = 0;      nBusy = 0;
      this.busyness = busyness;
      new NextPeriod(0).schedule (openingTime * HOUR);
      Sim.start();
      statArrivals.add ((double)nArrivals);
      statWaits.add (statWaitsDay.sum() / nServiceE);
   }
   
   //Simuler un jour avec le paramétre busyness 
   public void simulateOneDay () {
      simulateOneDay (GammaDist.inverseF (alpha0, alpha0, 8,
                                          streamB.nextDouble()));
   }
   
   /**
    * startA() qui permet de démarrer la simulation pour les 1000 jours indiqués <br>
    * Elle permet d'afficher le rapport final pour les clients de type A <br>
    * Elle affiche en plus l'histogramme <br>
    * @throws IOException
    */
   public void startA() throws IOException {
	   		
	   //Simuler pour 1000 jours
	      for (int i = 0; i < 1000; i++)  simulateOneDay();
	      System.out.println ("\nNum. service expected = " + nServiceE +"\n");
	      for (int i = 0; i < allTal.length; i++) {
	         allTal[i].setConfidenceIntervalStudent();
	         allTal[i].setConfidenceLevel (0.90);
	      }
	      //Afficher les statistiques
	      System.out.println (Tally.report ("Bank A:", allTal));
	      HistogramChart hist = new HistogramChart("Histogramme A ",
	    		  "Temps d'attente moyen",
	    		  "Nombre d'observation",
	    		  ((TallyStore) statWaits).getArray(),
	    		  ((TallyStore) statWaits).numberObs()
	    	);
	      //Tracer l'histogramme
	      hist.view(800, 500);
	      
	   }
   
}


