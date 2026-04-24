import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class First {

    public static void main(String[] args) throws Exception {

        String regNo = "RA2311028010180";

        Set<String> seen = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        for (int i = 0; i < 10; i++) {

            String urlStr = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages?regNo="
                    + regNo + "&poll=" + i;

            boolean success = false;

            while (!success) {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    int code = conn.getResponseCode();

                    if (code != 200) {
                        System.out.println("Retrying poll " + i + "...");
                        Thread.sleep(5000);
                        continue;
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    JSONArray events = json.getJSONArray("events");

                    for (int j = 0; j < events.length(); j++) {

                        JSONObject event = events.getJSONObject(j);

                        String roundId = event.getString("roundId");
                        String participant = event.getString("participant");
                        int score = event.getInt("score");

                        String key = roundId + "-" + participant;

                        if (!seen.contains(key)) {
                            seen.add(key);

                            scores.put(participant,
                                    scores.getOrDefault(participant, 0) + score);
                        }
                    }

                    System.out.println("Poll " + i + " done");
                    success = true;

                } catch (Exception e) {
                    System.out.println("Error, retrying poll " + i);
                    Thread.sleep(5000);
                }
            }

            Thread.sleep(5000); // mandatory delay
        }

        // Leaderboard
        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());

        int total = 0;
        JSONArray leaderboard = new JSONArray();

        for (Map.Entry<String, Integer> entry : list) {

            JSONObject obj = new JSONObject();
            obj.put("participant", entry.getKey());
            obj.put("totalScore", entry.getValue());

            leaderboard.put(obj);
            total += entry.getValue();
        }

        System.out.println("Total Score: " + total);
        System.out.println("Leaderboard: " + leaderboard.toString());

        // SUBMIT
        URL url = new URL("https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/submit");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject submit = new JSONObject();
        submit.put("regNo", regNo);
        submit.put("leaderboard", leaderboard);

        OutputStream os = conn.getOutputStream();
        os.write(submit.toString().getBytes());
        os.flush();
        os.close();

        int submitCode = conn.getResponseCode();

        BufferedReader br;
        if (submitCode >= 200 && submitCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder responseSubmit = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            responseSubmit.append(line);
        }

        System.out.println("Submit Response: " + responseSubmit.toString());

        conn.disconnect();
    }
}