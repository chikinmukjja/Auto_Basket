import numpy as np
import pandas as pd

train = pd.read_csv('basketball_data.csv')

feature_name = ['hog','optical','right','left']
label_name = ['move']

train_X = train[feature_name]
train_y = train[label_name]

from sklearn.svm import LinearSVC
clf = LinearSVC(C=1.0)
clf.fit(train_X,train_y)
print clf.predict([[0,0,1,0]])

from sklearn.externals import joblib
joblib.dump(clf,'classify_model2',compress=3)
print 'end'
