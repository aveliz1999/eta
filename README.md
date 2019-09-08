# eta
Android app to keep location and estimated time of arrival to a destination updated in realtime.

The user can share their ETA code with others, who can then see the progress (both distance and time) in real-time with the [ETA Website](https://github.com/aveliz1999/eta-web)

### Install Instructions
* `git clone https://github.com/aveliz1999/eta`
* `cd eta`
* Download your firebase project's google-services.json to the app/ directory
* Build and sign the APK or bundle

### Configuration
* Have or create a [Firebase](https://firebase.google.com/) account
* Set up [eta-functions](https://github.com/aveliz1999/eta-functions) with your Firebase account to handle distance and time calculations
* Set up [eta-web](https://github.com/aveliz1999/eta-web) to share and display the ETAs in realtime
* Change the `/res/values/strings.xml` value `website_path` to your eta-web url, ending with a `/`
* Set your Firebase Firestore database rules to:
```
rules_version = '2';
service cloud.firestore {
    match /databases/{database}/documents {
        match /etas/{eta} {
            allow list: if false;
            allow get: if true;
            allow create: if request.resource.data.keys().size() == 4 &&
                             request.resource.data.keys().hasAll(['creator', 'location', 'target', 'updated']) &&
                             request.resource.data.creator == request.auth.uid &&
                             request.resource.data.location is latlng &&
                             request.resource.data.target is latlng &&
                             request.resource.data.updated is timestamp &&
                             request.resource.data.updated == request.time;
        }
    }
}
```