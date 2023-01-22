package app.fedilab.android.peertube.helper;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */


import java.util.HashMap;
import java.util.LinkedHashMap;


public class HelperAcadInstance {


    public static String LOCAL = "LOCALES";
    public static String DISCOVER = "DECOUVRIR";
    public static String MOSTLIKED = "PLUS_APPRECIEES";
    public static String RECENTLY_ADDED = "AJOUTE_RECEMMENT";
    public static String TRENDING = "TENDANCE";
    public static String HISTORY = "HISTORIQUE";
    public static String SUBSCRIPTIONS = "ABONNEMENTS";
    public static String MYVIDEOS = "VIDEOS";
    public static String openIdURL = "https://auth.apps.education.fr/auth/realms/apps/protocol/openid-connect/auth?";

    //List of available emails
    public static String[] valideEmails = {
            "ac-aix-marseille.fr",
            "ac-amiens.fr",
            "ac-besancon.fr",
            "ac-bordeaux.fr",
            "clermont-ferrand.fr",
            "ac-corse.fr",
            "ac-creteil.fr",
            "ac-dijon.fr",
            "ac-grenoble.fr",
            "education.fr",
            "ac-guadeloupe.fr",
            "ac-guyane.fr",
            "ac-reunion.fr",
            "ac-lille.fr",
            "ac-limoges.fr",
            "ac-lyon.fr",
            "ac-martinique.fr",
            "ac-mayotte.fr",
            "ac-montpellier.fr",
            "ac-nancy.fr",
            "ac-nantes.fr",
            "ac-normandie.fr",
            "ac-orleans-tours.fr",
            "ac-paris.fr",
            "ac-poitiers.fr",
            "ac-rennes.fr",
            "ac-spm.fr",
            "ac-strasbourg.fr",
            "ac-toulouse.fr",
            "ac-versailles.fr",
            "ac-wf.wf",
            "monvr.pf",
            "ac-noumea.nc",
            "education.gouv.fr",
            "igesr.gouv.fr"
    };


    public static LinkedHashMap<String, String> instances_themes;
    public static HashMap<String, String> instance_client_id;

    static {
        instances_themes = new LinkedHashMap<>();
        instances_themes.put("Institutionnel Éducatif", "tube-institutionnel.apps.education.fr");
        instances_themes.put("Maternelle Éducatif", "tube-maternelle.apps.education.fr");
        instances_themes.put("Art & Sciences Humaines", "tube-arts-lettres-sciences-humaines.apps.education.fr");
        instances_themes.put("Sciences & Technologies", "tube-sciences-technologies.apps.education.fr");
        instances_themes.put("Éducation Physique & Sportive", "tube-education-physique-et-sportive.apps.education.fr");
        instances_themes.put("Enseignement Professionnel", "tube-enseignement-professionnel.apps.education.fr");
        instances_themes.put("Langues Vivantes", "tube-langues-vivantes.apps.education.fr");
        instances_themes.put("Action Éducative", "tube-action-educative.apps.education.fr");
        instances_themes.put("Cycle-2 Éducatif", "tube-cycle-2.apps.education.fr");
        instances_themes.put("Cycle-3 Éducatif", "tube-cycle-3.apps.education.fr");
    }

    static {
        instance_client_id = new HashMap<>();
        instance_client_id.put("tube-institutionnel.apps.education.fr", "tube-institutionnel");
        instance_client_id.put("tube-maternelle.apps.education.fr", "tube-maternelle");
        instance_client_id.put("tube-arts-lettres-sciences-humaines.apps.education.fr", "tube-arts-lettres-sciences-humaines");
        instance_client_id.put("tube-sciences-technologies.apps.education.fr", "tube-sciences-technologies");
        instance_client_id.put("tube-education-physique-et-sportive.apps.education.fr", "tube-education-physique-et-sportive");
        instance_client_id.put("tube-enseignement-professionnel.apps.education.fr", "tube-enseignement-professionnel");
        instance_client_id.put("tube-langues-vivantes.apps.education.fr", "tube-langues-vivantes");
        instance_client_id.put("tube-action-educative.apps.education.fr", "tube-action-educative");
        instance_client_id.put("tube-cycle-2.apps.education.fr", "tube-cycle-2");
        instance_client_id.put("tube-cycle-3.apps.education.fr", "tube-cycle-3");
    }


}
