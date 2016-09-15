import bluetooth
import RPi.GPIO as GPIO
import time
from sklearn.externals import joblib


def parsing_data(string_data):
	data_list = string_data.split(" ")
	hog = data_list[0]
	optical = data_list[1]
	return optical,hog

def server_init():
	# load classifier
	clf = joblib.load("classify_model2")
	# set GPIO, motor
	pin = 18
	GPIO.setmode(GPIO.BCM)
	GPIO.setup(pin,GPIO.OUT)
	motor = GPIO.PWM(pin,100)
	motor.start((90/180+1)*5)
	# set bluetooth socket
	server_socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
	port = 0
	server_socket.bind(("",port))
	server_socket.listen(1)
	return motor, server_socket, clf

def server_wait(server_socket):
	# wait client
	client_socket, address = server_socket.accept()
	print "Log : Accepted connection from", address
	return client_socket

def server_ready(client_socket):
	# wait video start
	while True:
		message = client_socket.recv(1024)
		if message == "start":
			print "Log : start video"
			return False
		if message == "exit":
			print "Log : exit"
			return True

def server_move(motor, client_socket, clf, start_state):
	cur_state = start_state
	while True:
		features = client_socket.recv(1024)
		if featrues == "Log : stop":
			return True
		optical, hog = parsing_data(features)
		pred = clf.predict([float(hog),float(optical),cur_state,1-cur_state])
		#right:1 , left:-1 , not move:0
		if pred == 1:
			print "Log : move right"
			angle = 90
			motor.ChangeDutyCycle((angle/180+1)*1.5)
			cur_state = 1
		elif pred == -1:
			print "Log : move left"
			angle = 270
			motor.ChangeDutyCycle((angle/180+1)*1.5)
			cur_state = 0
		else :
			print "Log : not move"


			

if __name__ == '__main__':

	print "Log : server power on"
	motor, server_socket,clf = server_init()
	print "Log : server initiated"
	while True:
		print "Log : server waiting connection..."
		client_socket = server_wait(server_socket)
		print "Log : client connected"
		while True:
			print "Log : server waiting video start"
			exit = server_ready(client_socket)
			if exit:
				client_socket.close()
				continue
			if server_move(motor,client_socket,clf,1):
				print "Log : video stop"
				

	




#try:
#	while True:
	
#		data = client_sock.recv(1024)
		print "received angle[%s]" % data
#		hog, optical = parsing_data(data)
#		print optical,hog
		
#		pred=clf.predict([float(hog),float(optical),1,0]) 
		print pred		
#		if pred == 0:
#			angle = 

		#if data.lstrip('-').isdigit():
		#	data = int(data)
		#	if (data > 100) | (data < -100) :			
		#		if data > 0:
		#			angle = 80/10.0 + 2.5
		#		
		#		else: 
		#			angle = 160/10.0 + 2.5
		#		
		#		p.ChangeDutyCycle(angle)
		#		time.sleep(0.5)
		#	
		#else: print "not angle"
		
#except KeyboardInterrupt:
#	p.stop()
#	GPIO.cleanup()



#client_sock.close()
#server_sock.close()
