/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.Tuple3;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DsnBrutCode {

    final List<Tuple3<String, String, String>> l;

    public enum DsnTypeCodeBrut {
        REMUNERATION("Rémunération"), AUTRE("Autre"), PRIME("Prime");

        private final String name;

        DsnTypeCodeBrut(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    public DsnBrutCode() {
        l = new ArrayList<Tuple3<String, String, String>>();
        // l.add(Tuple3.create("001", "Rémunération brute non plafonnée",
        // DsnTypeCodeBrut.REMUNERATION.getName()));
        l.add(Tuple3.create("010", "Salaire de base", DsnTypeCodeBrut.REMUNERATION.getName()));
        l.add(Tuple3.create("011", "Heures supplémentaires ou complémentaires", DsnTypeCodeBrut.REMUNERATION.getName()));
        l.add(Tuple3.create("012", "Heures d’équivalence", DsnTypeCodeBrut.REMUNERATION.getName()));
        l.add(Tuple3.create("013", "Heures d’habillage, déshabillage, pause", DsnTypeCodeBrut.REMUNERATION.getName()));

        l.add(Tuple3.create("001", "Indemnité spécifique de rupture conventionnelle", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("002", "Indemnité versée à l'occasion de la cessation forcée des fonctions des mandataires sociaux", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("003", "Indemnité légale de mise à la retraite par l'employeur", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("004", "Indemnité conventionnelle de mise à la retraite par l'employeur", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("005", "Indemnité légale de départ à la retraite du salarié", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("006", "Indemnité conventionnelle de départ à la retraite du salarié", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("007", "Indemnité légale de licenciement", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("008", "Indemnité légale supplémentaire de licenciement", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("009", "Indemnité légale spéciale de licenciement", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("010", "Indemnité légale spécifique de licenciement", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("011", "Indemnité légale de fin de CDD", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("012", "Indemnité légale de fin de mission", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("013", "Indemnité légale due aux journalistes", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("014", "Indemnité légale de clientèle", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("015", "Indemnité légale due au personnel naviguant de l'aviation civile", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("016", "Indemnité légale versée à l'apprenti", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("017", "Dommages et intérêts dus à un CDD", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("018", "Indemnité due en raison d'un sinistre", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("019", "Indemnité suite à clause de non concurrence", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("020", "Indemnité compensatrice de congés payés", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("021", "Indemnité conventionnelle (supplémentaire aux indemnités légales)", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("022", "Indemnité transactionnelle (supplémentaire aux indemnités conventionnelles)", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("023", "Indemnité compensatrice de préavis payé non effectué", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("025", "Indemnité compensatrice des droits acquis dans le cadre d’un compte épargne temps", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("026", "Prime exceptionnelle liée à l'activité avec période de rattachement spécifique", DsnTypeCodeBrut.PRIME.getName()));

        l.add(Tuple3.create("027", "Prime liée à l'activité avec période de rattachement spécifique", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("028", "Prime non liée à l'activité", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("029", "Prime liée au rachat des jours de RTT avec période de rattachement spécifique", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("030", "Prime rachat CET", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("031", "Prime de partage de profits avec période de rattachement spécifique", DsnTypeCodeBrut.PRIME.getName()));
        l.add(Tuple3.create("032", "Indemnité compensatrice de fin de contrat pour inaptitude suite AT ou Maladie Professionnelle", DsnTypeCodeBrut.PRIME.getName()));

        l.add(Tuple3.create("01", "Somme versée par un tiers", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("02", "Avantage en nature : repas", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("03", "Avantage en nature : logement", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("04", "Avantage en nature : véhicule", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("05", "Avantage en nature : NTIC", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("06", "Avantage en nature : autres", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("07", "Frais professionnels remboursés au forfait", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("08", "Frais professionnels pris en charge par l'employeur", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("09", "Frais professionnels remboursés au réel", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("10", "Déduction forfaitaire spécifique", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("11", "Participation y compris supplément", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("12", "Intéressement y compris supplément", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("14", "Abondement au plan d'épargne entreprise", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("15", "Abondement au plan d'épargne interentreprises", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("16", "Abondement au plan d'épargne pour la retraite collectif", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("17", "Participation patronale au financement des titres-restaurant", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("18", "Participation patronale aux frais de transports publics", DsnTypeCodeBrut.AUTRE.getName()));
        l.add(Tuple3.create("19", "Participation patronale aux frais de transports personnels", DsnTypeCodeBrut.AUTRE.getName()));

    }

    public List<Tuple3<String, String, String>> getCodes() {
        return l;
    }

    public void insertCode(SQLTable table) throws SQLException {

        for (Tuple3<String, String, String> tuple3 : l) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", tuple3.get0());
            rowVals.put("NOM", tuple3.get1());
            rowVals.put("TYPE", tuple3.get2());
            rowVals.insert();
        }
    }

}
