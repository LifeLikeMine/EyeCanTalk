import mlflow
import mlflow.keras
from flask import Flask, request, jsonify
from flaskext.mysql import MySQL

app = Flask(__name__)
mysql = MySQL()

# base_model_path = "base_model_path"

# base_model = mlflow.keras.load_model(base_model_path)

# MySQL configurations
app.config['MYSQL_DATABASE_USER'] = 'admin'
app.config['MYSQL_DATABASE_PASSWORD'] = 'lch63877812'
app.config['MYSQL_DATABASE_DB'] = 'mydb'
app.config['MYSQL_DATABASE_HOST'] = 'mydb.c9ibzimhazfs.ap-northeast-2.rds.amazonaws.com'

mysql.init_app(app)

@app.route('/add-user', methods=['POST'])
def add_user():
    user_id = request.form['user_id']

    # 새로운 사용자에 따라 베이스 모델 등록
    user_model_path = f"models/{user_id}"
    # mlflow.keras.log_model(base_model, user_model_path)

    return f"User {user_id} model has been added."


@app.route('/recommend', methods=['GET'])
def AAC_recommend():
    user_id = request.args.get('user_id')
    aac_id = request.args.get('aac_id')
    
    conn = mysql.connect()
    cursor = conn.cursor()
    aac_id_str = ",".join(map(str, aac_id))

    insertion_query = "INSERT INTO table_name (user_id, aac_id) VALUES (%s, %s)"
    cursor.execute(insertion_query, (user_id,aac_id_str))

    conn.commit()  # Save the changes
    conn.close()

    # mlflow에서 user_id 보고 모델 가져오기
    model_uri = f""
    model = mlflow.keras.load_model(model_uri)

    # 모델 돌려서 나온 aac_id 리턴
    recommend_aac_id = model.predict(aac_id)

    return jsonify(recommend_aac_id)

@app.route('/')
def hello():
    return ''


if __name__ == "__main__":
    app.run(debug=True)
