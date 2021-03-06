package com.example.myapplication;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class apiEtis{
   private String session_id = null;

   public apiEtis(){}
   public apiEtis(String session_id){
      this.session_id = session_id;
   }

   //----------------

   public class subject{
      public final String n;
      public final String time;
      public final String title;
      public final String teacher;
      public final String auditorium;

      public subject(String n, String time, String title, String teacher, String auditorium) {
         this.n = n;
         this.time = time;
         this.title = title;
         this.teacher = teacher;
         this.auditorium = auditorium;
      }
   }

   public class day{
      public final String title;
      public final ArrayList<subject> subjects;

      public day(String title, ArrayList<subject> subjects) {
         this.title = title;
         this.subjects = subjects;
      }
   }

   public ArrayList<day> getTimeTable(boolean cons, int week) throws IOException {
      String server_response = getPage("https://student.psu.ru/pls/stu_cus_et/stu.timetable?p_cons="+(cons?'y':'n')+"&p_week="+ week);
      if(server_response != null) {

         System.out.println(server_response.length());
         /*
         Document doc = Jsoup.connect("https://student.psu.ru/pls/stu_cus_et/stu.timetable?p_cons="+(cons?'y':'n')+"&p_week="+ week)
                 .cookie(session_id.split("=")[0], session_id.split("=")[1])
                 .execute().parse();
          */
         Document doc = Jsoup.parse(server_response);
         Elements days = doc.getElementsByClass("day");

         ArrayList<day> res = new ArrayList<>();

         for (Element day : days) {
            ArrayList<subject> subjects = new ArrayList<>();
            Elements table = day.getElementsByTag("tr");
            for (Element row : table) {

               String title, teacher, auditorium;
               if(row.getElementsByClass("pair_info").get(0).childrenSize() > 0) {
                  title = row.getElementsByClass("dis").first().text();
                  teacher = row.getElementsByClass("teacher").get(0).getElementsByTag("a").get(0).text();
                  auditorium = row.getElementsByClass("aud").get(0).text();
               }
               else
                  title = teacher = auditorium = null;

               subjects.add(new subject(
                       row.getElementsByClass("pair_num").first().ownText(),
                       row.getElementsByClass("eval").first().text(),
                       title, teacher, auditorium
               ));
            }

            res.add(new day(day.getElementsByTag("h3").text(), subjects));
         }
         return res;
      }
      return null;
   }

   //----------------

   public String getRatingCurrent(){
      try {
         URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.signs?p_mode=current");
         HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
         connection.setRequestProperty("Cookie", session_id);

         if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            return readStream(connection.getInputStream());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      return null;
   }

   public String getRatingSession(){
      try {
         URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.signs?p_mode=session");
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.setRequestProperty("Cookie", session_id);

         if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK){
            return readStream(connection.getInputStream());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      return null;
   }

   public String getRatingDiplom(){
      try {
         URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.signs?p_mode=diplom");
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.setRequestProperty("Cookie", session_id);

         if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK){
            return readStream(connection.getInputStream());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public String getCurriculumShort(){
      try {
         URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.teach_plan?p_mode=short");
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.setRequestProperty("Cookie", session_id);

         if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK){
            return readStream(connection.getInputStream());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   //----------------

   public class absence{
      final public int n;
      final public String[] dates;
      final public String discipline;
      final public String typeWork;
      final public String lecturer;

      public absence(int n, String[] dates, String discipline, String typeWork, String lecturer){
         this.n = n;
         this.dates = dates;
         this.discipline = discipline;
         this.typeWork = typeWork;
         this.lecturer = lecturer;
      }
   }

   public class MissedClasses {
      final public String title;
      final public ArrayList<absence> absences;

      public MissedClasses(String title, ArrayList<absence> absences) {
         this.title = title;
         this.absences = absences;
      }
   }

   public ArrayList<absence> getAbsence(int p_term) throws IOException {
      String server_response = getPage("https://student.psu.ru/pls/stu_cus_et/stu.absence?p_term="+p_term);
      if(server_response != null) {
         Document doc = Jsoup.parse(server_response);
         Elements tables = doc.getElementsByClass("slimtab_nice").get(0).getElementsByTag("tr");

         ArrayList<absence> list = new ArrayList<>();
         for (Element row : tables) {
            Elements els = row.getElementsByTag("td");
            if (els.size() == 5) {
               list.add(
                       new absence(
                               Integer.parseInt(els.get(0).text()),
                               els.get(1).text().split(" "),
                               els.get(2).text(),
                               els.get(3).text(),
                               els.get(4).text()
                       )
               );
            }
         }
         return list;
      }
      return null;
   }

   public ArrayList<MissedClasses> getMissedClasses() throws IOException {
      String[] p = new String[] {"осенний триместр", "весенний триместр", "летний триместр"};
      ArrayList<MissedClasses> temp = new ArrayList<>();
      for(int i=0; i < p.length; i++)
         temp.add(new MissedClasses(p[i], getAbsence(i+1)));
      return temp;
   }

   //----------------

   public static class Rating{
      public final String title;
      public final RatingRow[] rows;

      Rating(String title, RatingRow[] rows) {
         this.title = title;
         this.rows = rows;
      }

      public static class RatingRow{
         public final String combination;
         public final String ranking;

         RatingRow(String combination, String ranking) {
            this.combination = combination;
            this.ranking = ranking;
         }
      }
   }

   public ArrayList<Rating> getRating() throws IOException{
      ArrayList<Rating> res = new ArrayList<>();
      String server_response = getPage("https://student.psu.ru/pls/stu_cus_et/stu.signs?p_mode=rating");
      if(server_response != null) {
         Document doc = Jsoup.parse(server_response);
         Elements item = doc.getElementsByClass("submenu").get(1).getElementsByClass("submenu-item");
         for(int i=item.size()-1; i >= 0; i--){
            Element doc2 = null;

            if(item.get(i).childrenSize() == 1){
               String server_response2 = getPage("https://student.psu.ru/pls/stu_cus_et/"+item.get(i).child(0).attr("href"));
               if(server_response2 != null)
                  doc2 = Jsoup.parse(server_response2).getElementsByClass("common").get(0);
            }
            else{
               doc2 = doc.getElementsByClass("common").get(0);
            }

            Elements tr = doc2.getElementsByTag("tr");
            Rating.RatingRow[] rr = new Rating.RatingRow[2];
            for(int j = 0; j < 2 ; j++){
               Elements td = tr.get(j+1).getElementsByTag("td");
               rr[j] = new Rating.RatingRow(td.get(0).text(), td.get(1).text());
            }
            res.add(new Rating(item.get(i).text(), rr));
         }
      }
      return res;
   }

   //----------------

   public String getOrders(){
      try {
         URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.orders");
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.setRequestProperty("Cookie", session_id);

         if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            return readStream(connection.getInputStream());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public ArrayList<String> getAnnounce() throws IOException{
      ArrayList<String> res = new ArrayList<>();
      String server_response = getPage("https://student.psu.ru/pls/stu_cus_et/stu.announce");
      if(server_response != null) {
         Document doc = Jsoup.parse(server_response);
         Elements messages = doc.getElementsByClass("msg");
         for(Element msg : messages){
            StringBuilder temp = new StringBuilder();
            for(Element li : msg.getElementsByTag("li"))
               temp.append(temp.length() > 0 ? "<br>" : "").append(li.html());
            // delete hyphens at the end
            res.add(Pattern.compile("[(?:<br>)\\s]+$").matcher(temp.toString()).replaceAll(""));
         }
      }
      return res;
   }

   //-----------------

   public String getTeacherMessages(){
      try {
         return getPage("https://student.psu.ru/pls/stu_cus_et/stu.teacher_notes");
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public String getElectricRes(){
        try {
            URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.electr");
            HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
            connection.setRequestProperty("Cookie", session_id);

            if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                return readStream(connection.getInputStream());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

   public String getEducationalStandard(){
      try {
         URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.electr");
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.setRequestProperty("Cookie", session_id);

         if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            return readStream(connection.getInputStream());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public String getTeachers(){
      try {
         URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.teachers");
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.setRequestProperty("Cookie", session_id);

         if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            return readStream(connection.getInputStream());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public String auth(String login, String password) throws IOException {
        /*
            https://student.psu.ru/pls/stu_cus_et/stu.login

            Host: student.psu.ru
            User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:72.0) Gecko/20100101 Firefox/72.0
            Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*\/*;q=0.8
            Accept-Language: ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3
            Accept-Encoding: gzip, deflate, br
            Content-Type: application/x-www-form-urlencoded
            Content-Length: 72
            Origin: https://student.psu.ru
            DNT: 1
            Connection: keep-alive
            Referer: https://student.psu.ru/pls/stu_cus_et/stu.teach_plan
            Upgrade-Insecure-Requests: 1

            p_redirect	stu.teach_plan
            p_username	%D1%FB%F7%E5%E2
            p_password	pass
        */

      URL url = new URL("https://student.psu.ru/pls/stu_cus_et/stu.login");
      HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();

      // Send post request
      String params = "p_redirect=stu.teach_plan&p_username="+URLEncoder.encode(login, "windows-1251")+"&p_password="+password;
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.writeBytes(params);
      wr.flush();
      wr.close();

      int responseCode = connection.getResponseCode();
      if(responseCode == HttpsURLConnection.HTTP_OK){
         String server_response = readStream(connection.getInputStream());
         /**/System.out.println("Data page : " + server_response);
      }

      //List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
      String SetCookie = connection.getHeaderField("Set-Cookie");
      if(SetCookie != null)
         this.session_id = SetCookie.split(";")[0];

      return this.session_id;
   }

   public String getPage(String strUrl) throws IOException {
      URL url = new URL(strUrl);
      HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
      connection.setRequestProperty("Cookie", session_id);

      if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK)
         return readStream(connection.getInputStream());
      return null;
   }

   private static String readStream(InputStream in) {
      BufferedReader reader = null;
      StringBuffer response = new StringBuffer();
      try {
         reader = new BufferedReader(new InputStreamReader(in, "windows-1251"));
         String line;
         while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
         }
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         if (reader != null) {
            try {
               reader.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
      return response.toString();
   }
}