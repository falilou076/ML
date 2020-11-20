package tutorial;

import java.io.IOException;

/**
 * 
 * @author Mohamet Tall && Falilou Fall && Mame Diarra Sow <br>
 *Cette classe permet de simuler l'ensemble de la banque pour les cleints de types A et B<br>
 *On simule d'abord les cleints de types B et on r�cup�re la liste des temps pour lesquels les conseillers sont libres<br>
 *On passe cette liste en argument au constructeur de Bank_A <br>
 *On initialise Bank_A en lui passant en param�tre le chemin du fichier et la liste qui contient les temps libre <br>
 *On simule A <br>
 *Dans la simulation de A, on g�re en m�me temps le fait que les conseillers peuvent servir les clients de type A <br>
 *si ces derniers sont libres. <br>
 */
public class Simulate {

	public static void main(String[] args) throws IOException {
		//On initialise BankBf
		BankBf b = new BankBf();
		//On d�marre la simulation et on affiche les r�sultats pour les clients de type B
		b.startB();
		//On initialise Bank_A
		Bank_A a = new Bank_A("C:/Users/HP/eclipse-workspace/File/src/tutorial/Bank.dat", b.temps_libre);
		//On d�marre la simulation et on affiche les r�sultats pour les clients de type A
		a.startA();
		
	}

}
