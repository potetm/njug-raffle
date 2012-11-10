@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.5.2')
import groovyx.net.http.*
import groovy.json.JsonSlurper
import java.util.Random;

def API_KEY     = '2065417e4b50326e73395b1973dd76'
def groupName   = 'nashvillejug'
def httpBuilder = new HTTPBuilder('http://api.meetup.com')
def random      = new Random()
def events;

ArrayList.metaClass.removeRand = { 
	if (delegate.size) delegate.remove(new Random().nextInt(delegate.size() ?: 1))
}

// Since we're reading from stdin in two closures, we need to keep the same BufferedReader open for the duration of the script
def reader = System.in.newReader()
reader.metaClass.readLine = { prompt -> println prompt; readLine() }

def getWinners
getWinners = { eventList ->
	def winner = eventList?.removeRand()?.member?.name
	if (winner) {
		println "And the raffle winner is... ${winner}!"

		if (reader.readLine("Another drawing? [y/n]") == "y") getWinners(eventList)
	} else {
		println "Out of winners tonight"
	}
}

def holdRaffle = { event ->
	httpBuilder.get(
		path: '/2/rsvps',
		query: [
					 key: API_KEY,
					sign: true,
					rsvp: 'yes',
			event_id: event.id,
					page: 20
		]
	) { eventsResponse, eventsJson ->
			def attendees = new JsonSlurper().parseText(eventsJson.toString()).results.asList()
			println "${attendees.size()} attendee(s) found..." 
			getWinners(attendees) }}

httpBuilder.get(
	path: '/2/events',
	query: [
		          key: API_KEY,
		         sign: true,
		         page: 20,
		group_urlname: groupName,
		       status: "upcoming"
	]
) { eventsResponse, eventsJson ->
		events = new JsonSlurper().parseText(eventsJson.toString()).results.asList()
		println "${events.size()} events(s) found..." }

events.each { event ->
	if (reader.readLine("Get winners for event ${event.name}?  [y/n]") == "y") holdRaffle(event) }

