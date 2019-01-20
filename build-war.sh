(cd frontend; npx webpack)
cp frontend/index.html backend/src/main/resources/static/index.html
cp frontend/images/* backend/src/main/resources/static/images/
cp frontend/dist/bundle.js backend/src/main/resources/static/dist/bundle.js
cp frontend/dist/index.css backend/src/main/resources/static/dist/index.css
(cd backend; gradle bootWar)
mv backend/build/libs/groove-0.0.1-SNAPSHOT.war .
