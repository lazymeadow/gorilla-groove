(cd frontend; npx webpack)

mkdir --parents backend/src/main/resources/static/dist
mkdir --parents backend/src/main/resources/static/images
mkdir --parents backend/src/main/resources/static/node_modules/react-toastify/dist

cp frontend/index.html backend/src/main/resources/static/index.html
cp frontend/images/* backend/src/main/resources/static/images/
cp frontend/dist/bundle.js backend/src/main/resources/static/dist/bundle.js
cp frontend/dist/index.css backend/src/main/resources/static/dist/index.css
cp frontend/node_modules/react-toastify/dist/ReactToastify.min.css backend/src/main/resources/static/node_modules/react-toastify/dist/ReactToastify.min.css

(cd backend; gradle bootWar)

mv backend/build/libs/groove-0.0.1-SNAPSHOT.war .
