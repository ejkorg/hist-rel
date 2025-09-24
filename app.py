import os
from flask import Flask, render_template, jsonify, request, json
import sys, oracledb
from datetime import datetime



def start_pool(user,password,dns,port):
    pool = oracledb.create_pool(
        user=user,
        password=password,
        dsn=dns,
        port=port,
        min=2,
        max=12,
        increment=1,
        timeout=4
    )
    return pool

class db_config:
    def __init__(self, host, user, password, port):
        self.host = host
        self.user = user
        self.password = password
        self.port = port

    @property
    def host(self):
        return self._host

    @host.setter
    def host(self, value):
        self._host = value

    @property
    def user(self):
        return self._user

    @user.setter
    def user(self, value):
        self._user = value

    @property
    def password(self):
        return self._password
    @password.setter
    def password(self, value):
        self._password = value

    @property
    def port(self):
        return self.port

    @port.setter
    def port(self, value):
        self._port = value

app = Flask(__name__)


app.secret_key = os.getenv("RELOADER_FLASK_SECRET", "replace-me-with-secure-random-secret")



with open('dbconnections.json', 'r') as database_file:    
    database_list = json.load(database_file) 
    site_list= sorted(database_list.keys())

db_connection = {}


@app.route('/')
def main():    
    return render_template("config.html", sites=site_list)


@app.route("/location", methods =["GET","POST"])
def location():
    print ("Calling location")
    site = request.form['site']
    db_connection = extract_key_value(database_list, site)
    db_config.host = db_connection.get('host')
    db_config.user = db_connection.get('user')
    db_config.password = db_connection.get('password')
    db_config.port = db_connection.get('port')
    print(db_config.host, db_config.user, db_config.password)  
    locations = get_location()    
    
    return jsonify(locations)

@app.route("/datatype", methods =["GET","POST"])
def data_type():
    location = request.form['location']   
    datatypes = get_data_type(location)    
    
    return jsonify(datatypes)


@app.route("/testertype", methods =["GET","POST"])
def tester_type():
    datatype_id = request.form['datatype']
    #print(db_config.host, db_config.user, db_config.password)       
    testers = get_tester_types(datatype_id, db_config.user,db_config.password,db_config.host,db_config.port)
    #testers = get_tester_types(datatype_id, "DATAPORT_USER","useryqs231","phyqsp-db.onsemi.com/PHYQSP","1579")
    return jsonify(testers)

@app.route("/sender", methods=['GET','POST'])
def sender():
    #senderid = '1023'
    location = request.form['location']
    datatype_id = request.form['datatype']
    tester_id = request.form['testertype']
    print(f"sender param : loc {location} datatype {datatype_id} tester id {tester_id}" )
    senderid = get_sender(location,datatype_id,tester_id,db_config.user,db_config.password,db_config.host,db_config.port)       
    return (str(senderid))

@app.route("/save_config", methods=['GET','POST'])
def save_config():
    site = request.form['site']
    location = request.form['location']
    datatype = request.form['datatype']
    testertype = request.form['testertype']
    senderid = request.form['senderid']    
    start_date = request.form['start_date']
    end_date = request.form['end_date']
    emailadd = request.form['emailadd']
    jira = request.form['jira']
    frequency = request.form['frequency']
    my_start_date = str(datetime.strptime(start_date,'%Y-%m-%d').strftime('%d-%b-%Y')) + " 00.00.00.000000000 PM"
    my_end_date = str(datetime.strptime(end_date,'%Y-%m-%d').strftime('%d-%b-%Y')) + " 11.59.59.000000000 PM"
    config_name = "_".join([site,location,datatype,testertype])

   
            
    config_dictionary = {config_name : {"active":"true","site":site,"location":location,"data_type":datatype, "tester":testertype,"start_date": my_start_date,"end_date":my_end_date,"email":emailadd,"jira":jira, "frequency": frequency}}
    print(config_dictionary)
    fname="config.json"
    if os.path.isfile(fname):
       json_object = json.dumps(config_dictionary)
       with open("config.json","r+") as file:
            file_data = json.load(file)
            file_data.update(config_dictionary)
            file.seek(0)
            #file.write(json.dumps(file_data))
            json.dump(file_data, file, indent = 4)

    else:
         json_object = json.dumps(config_dictionary)
         with open("config.json","w") as outputfile:
            outputfile.write(json_object)
    #json_object = json.dumps(config_dictionary)
    # with open("config.json","a") as outputfile:
    #     outputfile.write(json_object)
    return("Done")

def extract_key_value(json_data, json_key):
    value = json_data.get(json_key)
    return value 

def get_location():
    
    locations=[]    
    pool = start_pool(db_config.user,db_config.password,db_config.host,db_config.port)
    with pool.acquire() as connection:
        try:
            cursor = connection.cursor()
            query_string = "select id,location from DTP_LOCATION order by location asc"

            #print(query_string)
            for row in cursor.execute(query_string):
                outputObj ={
                    'id': row[0],
                    'location':row[1]
                }
                locations.append(outputObj)
               
            cursor.close()
        except oracledb.errors as err:
            error_obj, = err.args
            #print(f"Error getting the data types: {error_obj.message}")
        finally:
            pool.release(connection)
            pool.close()     
    return locations

def get_data_type(location):
    # print("This is the user " + user )
    datatypes=[]    
    pool = start_pool(db_config.user,db_config.password,db_config.host,db_config.port)
    with pool.acquire() as connection:
        try:
            cursor = connection.cursor()
            #query_string = "select id,data_type from DTP_DATA_TYPE order by data_type asc"
            query_string =f"select location.id,data.data_type from DTP_DATA_TYPE data INNER JOIN DTP_LOCATION_DATA_TYPE location on data.id = location.id_data_type where location.id_location = '{location}' order by data.data_type asc"
            print(query_string)
            for row in cursor.execute(query_string):
                outputObj ={
                    'id': row[0],
                    'data_type':row[1]
                }
                datatypes.append(outputObj)
                #datatypes.insert(row[0],row[1])
                #print(row[1])
            cursor.close()
        except oracledb.errors as err:
            error_obj, = err.args
            #print(f"Error getting the data types: {error_obj.message}")
        finally:
            pool.release(connection)
            pool.close()
    
    
    return datatypes

def get_tester_types(datatype_id,user,password,dns,port):
    testers =[]
    pool = start_pool(user,password,dns,port)
    with pool.acquire() as connection:
        try:
            cursor = connection.cursor()
            query_string =f"select datatype.id_tester_type, tester.type from dtp_tester_type tester join dtp_tester_data_type datatype on tester.id = datatype.id_tester_type where id_data_type_mapping = '{datatype_id}' order by tester.type asc"
            #query_string="select id, type from DTP_TESTER_TYPE"
            #print(query_string)
            for row in cursor.execute(query_string):
                #print("Tester :" + str(row))
                outputObj ={
                    'id': row[0],
                    'testertype':row[1]
                }
                testers.append(outputObj)
            #print("DONE")
            cursor.close()
        except oracledb.errors as err:
             error_obj, = err.args
             #print(f"Error getting the tester types: {error_obj.message}")
        finally:
            pool.release(connection)
            pool.close()
        
        return testers
    

def get_sender(location_id,datatype_id,tester_id,user,password,dns,port):    
    pool = start_pool(user,password,dns,port)
    sender = 0
    with pool.acquire() as connection:
        try:
            cursor = connection.cursor()
            query_string =f"select id_sender from DTP_DIST_CONF where id_location='{location_id}' AND id_data_type = (select id_data_type from  DTP_LOCATION_DATA_TYPE where id = '{datatype_id}') AND id_tester_type = '{tester_id}'"
            #query_string="select id, type from DTP_TESTER_TYPE"
            print(query_string)
            for row in cursor.execute(query_string):
                print("Tester :" + str(row))
                sender = row[0]
            #print("DONE")
            cursor.close()
        except oracledb.errors as err:
             error_obj, = err.args
             #print(f"Error getting the tester types: {error_obj.message}")
        finally:
            pool.release(connection)
            pool.close()
        
        return sender
    

#main driver
if __name__ == '__main__':
    app.run(debug=True)