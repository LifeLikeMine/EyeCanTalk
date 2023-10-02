from flask import Flask, request, jsonify, g
from flaskext.mysql import MySQL
from flask_cors import CORS
import json
import pandas as pd
import numpy as np
import boto3
from keras.preprocessing.sequence import pad_sequences #문장길이통일
import os
from keras.models import load_model

app = Flask(__name__)
CORS(app) 
mysql = MySQL()
# model = load_model('/home/ubuntu/cd5f2549-6f14-4d9d-ac0a-76de39b46008.h5')
config = pd.read_csv("./config.csv")
app.config['MYSQL_DATABASE_USER'] = 'admin'
app.config['MYSQL_DATABASE_PASSWORD'] = config.loc[0,'Db password']
app.config['MYSQL_DATABASE_DB'] = 'aidb'
app.config['MYSQL_DATABASE_HOST'] = 'ai-db.cz2xtcn831o2.ap-northeast-2.rds.amazonaws.com'

max_len=20

def get_model(user_id):
    try:
        s3 = boto3.client(
        's3',
        aws_access_key_id=config.loc[0, 'Access key ID'],
        aws_secret_access_key=config.loc[0, 'Secret access key'])
        bucket = 'mlflowbucket123'
        local_dir = f'/home/ubuntu/{user_id}.h5'
        s3_dir = f'model/{user_id}.h5'
        s3.download_file(bucket, s3_dir, local_dir)
    
    except Exception as e:
        print(e)
    
    return local_dir

mysql.init_app(app)

def get_db():
    if 'db' not in g:
        g.db = mysql.connect()
    return g.db

@app.teardown_appcontext
def close_db(error):
    db = g.pop('db', None)
    if db is not None:
        db.close()

@app.route('/add-user', methods=['POST'])
def add_user():
    try:
        user_id = request.form['user_id']
        user_model_path = f"models/{user_id}"
    except Exception as e:
        return jsonify(error=str(e)), 500
    return f"User {user_id} model has been added."

@app.route('/recommend', methods=['POST'])
def AAC_recommend():
    try:
        data = request.get_json()
        aac_id = data['imageDataList']
        user_id = data['uuid']
        if len(aac_id)>1:
            db = get_db()
            cursor = db.cursor()
            aac_id_str = ",".join(map(str, aac_id))

            insertion_query = "INSERT INTO user_aac_history (user_id, aac_id) VALUES (%s, %s)"
            cursor.execute(insertion_query, (user_id,aac_id_str))
            db.commit()  # Save the changes

        aac_id = np.array([aac_id])
        aac_id = pad_sequences(aac_id,maxlen=max_len-1, padding='pre')
        aac_id = aac_id.reshape(aac_id.shape[0], aac_id.shape[1], 1)

        loaded_model_dir = get_model(user_id)
        model = load_model(loaded_model_dir)
        recommend_aac_id = model.predict(aac_id)

        top_k_indices = np.argsort(recommend_aac_id[0])[-3:]
        top_k_indices_list = top_k_indices.tolist()
        response_data = {
            "aac_id": top_k_indices_list,
        }
        return jsonify(response_data)

    except Exception as e :
        print(e)
        return jsonify(error=str(e)), 500

if __name__ == "__main__":
    app.run(debug=True)
