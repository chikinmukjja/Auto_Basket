import RPi.GPIO as GPIO
import time

pin = 18
GPIO.setmode(GPIO.BCM)
GPIO.setup(pin, GPIO.OUT)
p = GPIO.PWM(pin, 100)
p.start(7.5)
angle = 90
move = (angle/180 + 1)*5

try:
	while True:
		p.ChangeDutyCycle(move)
		time.sleep(1)
		p.ChangeDutyCycle(15)
		time.sleep(1)
		p.ChangeDutyCycle(2.5)
		time.sleep(3)

except KeyboardInterrupt:
	p.stop()
	GPIO.cleanup()
