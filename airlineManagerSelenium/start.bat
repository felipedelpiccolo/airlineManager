git pull
mvn clean install
git commit src/test/resources/db/flightsLogs.sqlite -m "update db"
git push