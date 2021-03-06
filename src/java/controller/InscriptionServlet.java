package controller;

import tools.JavaMailUtil;
import dao.Database;
import dao.PersonneDao;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import modele.Personne;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;

@WebServlet("/inscription")
public class InscriptionServlet extends HttpServlet {

  private static final String VUE_INDEX = "/index.jsp";
  private static final String VUE_FORM_CON = "/WEB-INF/connexion.jsp";
  private static final String VUE_FORM_INS = "/WEB-INF/inscription.jsp";
  private static final String VUE_ERREUR = "/WEB-INF/exception.jsp";
  private static final String VUE_VERIFY = "/WEB-INF/confirmationInscription.jsp";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    request.getRequestDispatcher(VUE_FORM_INS).forward(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String nom = request.getParameter("nom");
    String prenom = request.getParameter("prenom");
    String mail = request.getParameter("mail");
    String mdp = request.getParameter("mdp");
    String mdp2 = request.getParameter("mdp2");
    String vue = VUE_FORM_INS;                                //inscription.jsp
    Random random = new Random();                               // Instance d'un objet random
    random.nextInt(9999999);                                  // Selection aléatoire d'un nombre entre 0 est 9999999
    String jeton = DigestUtils.md5Hex("" + random);
    String msg;

    // nous faisons d'abord un test sur tous les champs du formulaire
    if (nom == null || nom.trim().isEmpty() || prenom == null || prenom.trim().isEmpty() || mail == null || mail.trim().isEmpty() || mdp == null || mdp.trim().isEmpty() || mdp2 == null || mdp2.trim().isEmpty()) {
      request.setAttribute("erreurLogin", "Veuillez renseigner tous les champs");
      // la vue reste à VUE_FORM_INS
    } else if (!mail.matches("(?:\\w|[\\-_])+(?:\\.(?:\\w|[\\-_])+)*\\@(?:\\w|[\\-_])+(?:\\.(?:\\w|[\\-_])+)+")) {
      request.setAttribute("erreurLogin", "Ce mail est invalide, veuillez saisir un autre");
      // la vue reste à VUE_FORM_INS
    } else if (!mdp.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$")) {
      request.setAttribute("erreurLogin", "<ul>\n"
              + "<p><u>Le mot de passe :</u></p>\n"
              + "<li> - doit contenir au moins 8 caractères</li>\n"
              + "<li> - doit au moins un chiffre</li>\n"
              + "<li> - doit contenir au moins un caractère spécial dans la liste : @#$%^&+=</li>\n"
              + "<li> - ne doit pas contenir des espaces</li>\n"
              + "</ul>");
      // la vue reste à VUE_FORM_INS    
    } else if (!mdp.equals(mdp2)) {
      request.setAttribute("erreurLogin", "Les mots de passe ne sont pas identiques ! merci de vérifier votre saisie");
      // la vue reste à VUE_FORM_INS    
    }

    try {
      PersonneDao.deletePersonBydate(new Timestamp(System.currentTimeMillis()));
      Personne p = new Personne(prenom, nom, mail, mdp, jeton);           // Instance d'un objet p avec un constructeur paramétré qui initialise l'attribut est_Actif à false et la date_insertion à l'haure et date du système 
      PersonneDao.insert(p);
      String texte = "Veuillez confirmez votre inscription en cliquant sur le lien ci-après :"
              + JavaMailUtil.getCompletePath("confirmationEmail?token=" + jeton, request);
      String sujet = "Confirmez votre inscription sur AGRIOTES";

// Dans la classe PersonneDao, nous avons des méthodes qui permettent de préparer des requêtes Sql qui seront exécutées côté base de données
      JavaMailUtil.sendMail(mail, nom, prenom, sujet, texte);                       // Dans la classe JavaMailUtil, nous avons l'implémentation de ma méthode sendMail() qui permet t'établie l'envoi du mail
      vue = VUE_VERIFY;                                                   // la valeur de vue change à "/WEB-INF/verify.jsp"
    } catch (SQLException ex) {
      try {
        Logger.getLogger(InscriptionServlet.class.getName()).log(Level.SEVERE, null, ex);
        switch (ex.getErrorCode()) {
          case Database.DOUBLON:
            boolean estConfirme = PersonneDao.estValide(mail);
            msg = (estConfirme) ? "Email déjà enregistré et confirmé"
                    : "Email déjà enregistré, veuillez confirmer votre inscription en cliquant sur le lien inclus dans le mail qui vous a été adressé";
            break;
          default:
            msg = "Pb avec la base de données "
                    + ex.getMessage(); // seulement en dev (pb de sécurité)
            vue = VUE_ERREUR;
        }
        request.setAttribute("erreurLogin", msg);
      } catch (SQLException ex1) {
        Logger.getLogger(InscriptionServlet.class.getName()).log(Level.SEVERE, null, ex1);
      }
    } catch (Exception ex) {
      Logger.getLogger(InscriptionServlet.class.getName()).log(Level.SEVERE, null, ex);
    }
    request.getRequestDispatcher(vue).forward(request, response);                   // La servlet nous envoi vers la vue appropriée en envoyant avec les objets request et response
  }

}
