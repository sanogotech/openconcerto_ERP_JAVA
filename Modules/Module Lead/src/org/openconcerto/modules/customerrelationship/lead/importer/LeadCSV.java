package org.openconcerto.modules.customerrelationship.lead.importer;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRowValues;

public class LeadCSV extends LineCSV {

    public final static int ID = 0;
    public final static int CODE = 1;
    public final static int NAME = 2;
    public final static int ID_ADR = 3;
    public final static int LOCALISATION = 4;
    public final static int PHONE = 5;
    public final static int MAIL = 6;
    public final static int CELL = 7;
    public final static int FAX = 8;
    public final static int CONTACT = 9;
    public final static int SECTEUR = 10;
    public final static int EFFECTIF = 11;
    public final static int ORIGINE = 12;
    public final static int SIRET = 13;
    public final static int APE = 14;
    public final static int COM = 15;
    public final static int STATUT = 16;
    public final static int PRENOM = 17;
    public final static int NOM = 18;
    public final static int CIVILITE = 19;
    public final static int DESC = 20;
    public final static int CONVERTI = 21;
    public final static int BUDGET = 22;
    public final static int DISPO = 23;
    public final static int CATEGORIE = 24;
    public final static int TYPE_TRAVAUX = 25;
    public final static int INFOS = 26;
    public final static int SIZE = 27;

    private Object idAdr;
    private String code, name, localisation, contact, phone, cell, mail, fax, com, origine, secteur, effectif, siret, ape;
    private String statut, prenom, nom, civilite, desc, converti, budget, dispo, categorie, typeT, infos;

    public LeadCSV(Object id, String name, String loc) {

        this.id = id;
        this.localisation = loc;
        this.name = name;
    }

    public void setInfos(String infos) {
        this.infos = infos;
    }

    public String getInfos() {
        return infos;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getBudget() {
        return budget;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCivilite(String civilite) {
        this.civilite = civilite;
    }

    public String getCivilite() {
        return civilite;
    }

    public String getConverti() {
        return converti;
    }

    public void setConverti(String converti) {
        this.converti = converti;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDispo() {
        return dispo;
    }

    public void setDispo(String dispo) {
        this.dispo = dispo;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getTypeT() {
        return typeT;
    }

    public void setTypeT(String typeT) {
        this.typeT = typeT;
    }

    public String getSiret() {
        return this.siret;
    }

    public void setSiret(String siret) {
        this.siret = siret;
    }

    public String getApe() {
        return ape;
    }

    public void setApe(String ape) {
        this.ape = ape;
    }

    public String getCell() {
        return cell;
    }

    public String getCom() {
        return com;
    }

    public void setCom(String com) {
        this.com = com;
    }

    public String getEffectif() {
        return effectif;
    }

    public void setEffectif(String effectif) {
        this.effectif = effectif;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public String getSecteur() {
        return secteur;
    }

    public void setSecteur(String secteur) {
        this.secteur = secteur;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getIdAdr() {
        return idAdr;
    }

    public void setIdAdr(Object idAdr) {
        this.idAdr = idAdr;
    }

    public String[] toCSVLine() {
        String[] values = new String[SIZE];

        values[ID] = this.id.toString();
        values[CODE] = this.code;
        values[NAME] = this.name;
        values[ID_ADR] = String.valueOf(this.idAdr);
        values[PHONE] = this.phone;
        values[MAIL] = this.mail;
        values[CELL] = this.cell;
        values[FAX] = this.fax;
        values[CONTACT] = this.contact;

        values[LOCALISATION] = this.localisation;
        values[SECTEUR] = this.secteur;
        values[EFFECTIF] = this.effectif;
        values[ORIGINE] = this.origine;
        values[COM] = this.com;
        values[APE] = this.ape;
        values[SIRET] = this.siret;

        values[STATUT] = this.statut;
        values[PRENOM] = this.prenom;
        values[NOM] = this.nom;
        values[CIVILITE] = this.civilite;
        values[DESC] = this.desc;
        values[CONVERTI] = this.converti;
        values[BUDGET] = this.budget;
        values[DISPO] = this.dispo;
        values[CATEGORIE] = this.categorie;
        values[TYPE_TRAVAUX] = this.typeT;
        values[INFOS] = this.infos;
        return values;
    }

    public SQLRowValues toSQLRowValues(DBRoot root, Map<Object, SQLRowValues> adresse) {
        SQLRowValues rowVals = new SQLRowValues(root.getTable("LEAD"));

        // rowVals.put("CODE", getCode());
        rowVals.put("COMPANY", getName());
        rowVals.put("ID_ADRESSE", adresse.get(getIdAdr()));

        rowVals.put("PHONE", getPhone());
        rowVals.put("EMAIL", getMail());
        rowVals.put("MOBILE", getCell());
        rowVals.put("FAX", getFax());
        rowVals.put("NAME", getContact());

        rowVals.put("LOCALISATION", getLocalisation());
        rowVals.put("INDUSTRY", getSecteur());
        try {
            rowVals.put("EMPLOYEES", Integer.valueOf(getEffectif()));
        } catch (Exception e) {
            System.err.println("Unable to parse " + getEffectif());
        }
        rowVals.put("SOURCE", getOrigine());
        int idCom = -1;
        if (getCom() != null) {
            if (getCom().toUpperCase().contains("YANN")) {
                idCom = 3;
            } else if (getCom().toUpperCase().contains("ERIC")) {
                idCom = 4;
            } else if (getCom().toUpperCase().contains("MARC")) {
                idCom = 2;
            }
        }
        if (idCom != -1) {
            rowVals.put("ID_COMMERCIAL", idCom);
        }
        rowVals.put("SIRET", getSiret());
        rowVals.put("APE", getApe());
        rowVals.put("DATE", new Date());

        int idCiv = 1;
        if (getCivilite() != null) {
            if (getCivilite().equalsIgnoreCase("Mr.")) {
                idCiv = 2;
            } else if (getCivilite().equalsIgnoreCase("Mrs.")) {
                idCiv = 3;
            }
        }
        if (idCiv != -1) {
            rowVals.put("ID_TITRE_PERSONNEL", idCiv);
        }
        rowVals.put("NAME", getNom());
        rowVals.put("FIRSTNAME", getPrenom());
        rowVals.put("INFORMATION", getDesc());
        // rowVals.put("REVENUE", (getBudget().trim().length()==0?0:Integer);
        rowVals.put("DISPO", getDispo());
        rowVals.put("INDUSTRY", getTypeT());
        rowVals.put("STATUS", getStatut());
        rowVals.put("INFOS", getInfos());

        if (getConverti().equalsIgnoreCase("1")) {
            SQLInjector inj = SQLInjector.getInjector(root.getTable("LEAD"), root.getTable("CLIENT"));
            SQLRowValues rowValsCli = inj.createRowValuesFrom(Arrays.asList(rowVals));

            SQLRowValues rowValsAdr = new SQLRowValues(rowVals.getForeign("ID_ADRESSE").asRowValues());
            // rowValsAdr.clearPrimaryKeys();
            rowValsCli.put("ID_ADRESSE", rowValsAdr);

            // Contact
            SQLRowValues rowValsContact = SQLInjector.getInjector(root.getTable("LEAD"), root.getTable("CONTACT")).createRowValuesFrom(Arrays.asList(rowVals));
            rowValsContact.put("ID_CLIENT", rowValsCli);
            rowVals.put("ID_CLIENT", rowValsCli);
        }

        return rowVals;
    }

    public boolean isDigitsOrChar(String s) {
        boolean ok = true;
        for (int i = 0; i < s.length(); i++) {
            final char charAt = s.charAt(i);
            ok &= ((charAt == ' ') || (charAt >= '0' && charAt <= '9') || (charAt >= 'a' && charAt <= 'z') || (charAt >= 'A' && charAt <= 'Z'));
        }
        return ok;
    }

    public static LeadCSV fromCSVLine(String[] line) {
        LeadCSV cli = new LeadCSV(line[ID], line[NAME], line[LOCALISATION]);
        cli.setCell(line[CELL]);
        cli.setContact(line[CONTACT]);
        cli.setFax(line[FAX]);
        cli.setIdAdr(line[ID_ADR]);
        cli.setMail(line[MAIL]);
        cli.setPhone(line[PHONE]);

        // cli.setLocalisation(line[LOCALISATION]);
        cli.setSecteur(line[SECTEUR]);
        cli.setEffectif(line[EFFECTIF]);
        cli.setOrigine(line[ORIGINE]);
        cli.setCom(line[COM]);

        cli.setSiret(line[SIRET]);
        cli.setApe(line[APE]);

        cli.setPrenom(line[PRENOM]);
        cli.setNom(line[NOM]);
        cli.setCivilite(line[CIVILITE]);
        cli.setDesc(line[DESC]);
        cli.setConverti(line[CONVERTI]);
        cli.setBudget(line[BUDGET]);
        cli.setDispo(line[DISPO]);
        cli.setCategorie(line[CATEGORIE]);
        cli.setTypeT(line[TYPE_TRAVAUX]);
        cli.setStatut(line[STATUT]);
        cli.setInfos(line[INFOS]);
        return cli;
    }
}
