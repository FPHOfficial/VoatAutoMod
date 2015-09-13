import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.chrome.ChromeDriver;

/*****************************************************************************\
 * AutoMod.java - automated moderator for Voat                               *
 *                                                                           *
 * Uses the Selenium WebDriver for automation                                *
 * Basic functions (auto tagging, deleting for banned words, auto posting)   *
 * can be done by modifying the global variables before main(). More         *
 * advanced functionality needs to be programmed in.                         *
 * Detailed information is in the README.txt, which should be included.      *
 * @author mikex5                                                            *
 * @version 0.2.2                                                            *
\*****************************************************************************/

public class AutoMod {
	
	// Name of the subverse, caps and lowercase matters
	static String subName = "";
	// Full URL of the sub to moderate, recommend pointing to the /new page
	static String mainPage = "http://voat.co/v/"+subName+"/new";
	
	// Username for the bot to use
	static String userName = "";
	// ...and password
	static String password = "";
	
	// Integer value of how many minutes to wait in before running again
	// (more active subs will want to set this value lower)
	static int waitTime = 9;
	
	// List of strings that will warrant removing posts or comments
	// WARNING: the strings are check without context eg. ho is detected in how
	// also, words should be lowercase since strings are converted to lowercase
	// GOOD FORMAT: {"badword","badword[ h]","bad string of words"};
	// Use regex like the middle to catch 'ass' or 'asshole' but not 'assassin'
	// BAD FORMAT: {"BadWord","BAD","WORD"};
	
	// List of strings not allowed in titles
	static String[] titleBan = {};
	// List of strings not allowed in submission messages
	static String[] messBan = {};
	// List of Strings not allowed in comments
	static String[] commBan = {};
	// List of web domains which cause a post to be deleted if linked to
	static String[] domainBan = {};
	// List of subverses which can cause a post to be deleted if linked to
	static String[] subBan = {};
	
	// Enum for the type of submissions allowed
	public enum PostType {TEXT, LINK, BOTH}
	// Set here TEXT/LINK for TEXT/LINK only, or BOTH if both allowed
	static PostType postMode = PostType.BOTH;
	
	// List of all flairs for the subverse, lowercase version of what users
	// put in the title, no square brackets needed
	static String[] flairs = {};
	// list of flair names, must correspond to above list, these are the flair
	// titles, case matters!
	static String[] flairList = {};
	
	// new standard date format, "year/month/day hour(1-24)/day-of-week"
	static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH/EEEE");
	
	/* ==WARNING! for each post on a special date, a boolean variable must be
	   added inside main(), and a bunch of other stuff needs to be uncommented.
	   Skip down to the todo to see more info==*/
	
	// list of special dates and times of when to post things
	static String[] specDates = {};
	// list of when to reset the date triggers, respectively
	// recommend resetting the triggers the next day
	static String[] dateResets = {};
	
	public static void main(String[] args) {
		
		// Booleans for special dates (so no double posting), be sure to
		// uncomment these and add more as needed
		boolean dated1 = false;
		//boolean dated2 = false;
		//etc...
		
		// Firefox webdriver by default, uncomment the line under to use Chrome
		WebDriver driver = new FirefoxDriver();
		//WebDriver driver = new ChromeDriver();
		
		// Go to the subverse to start
		driver.get(mainPage);
		
		// Wait for the page to load, sit through the DDoS protection screen
		waitForElement(driver,"submission");
		
		// Log in to the damn site
		System.out.println("logging in...");
		tooManyButtons(driver, "login-required", "login");
		waitForElement(driver,"form-group");
		driver.findElement(By.id("UserName")).sendKeys(userName);
		driver.findElement(By.id("Password")).sendKeys(password);
		driver.findElement(By.id("RememberMe")).click();
		List<WebElement> uselessButtons = driver.findElements(By.className(
				"col-md-4"));
		uselessButtons.get(uselessButtons.size()-1).findElement(By.className(
				"btn-whoaverse")).click();
		uselessButtons.clear();
		
		// Take inventory of all posts, assume everything abides by the rules
		System.out.println("taking inventory...");
		List<WebElement> goodPosts = driver.findElements(By.className(
				"submission"));
		// Put the submissions into a 2D list with id and # of comments
		ArrayList<ArrayList<String>> postList = new 
				ArrayList<ArrayList<String>>();
		for(int i = 0; i<goodPosts.size(); i++ ) {
			WebElement aPost = goodPosts.get(i);
			addToPostList(aPost, postList);
		}
		goodPosts.clear();
		
		// Separate the list into a new list based on age, posts older than a
		// day (by default) are ignored
		ArrayList<Integer> removalList = new ArrayList<Integer>();
		ArrayList<ArrayList<String>> oldPosts = 
				new ArrayList<ArrayList<String>>();
		for(int j = 0; j<postList.size(); j++ ) {
			ArrayList<String> postInfo = postList.get(j);
			WebElement thisPost = driver.findElement(By.className(
					"id-"+postInfo.get(0)));
			String timeTag = "";
			try {
				timeTag = thisPost.findElement(By.tagName("time"))
						.getText().split(" ")[1];
			} catch(java.lang.ArrayIndexOutOfBoundsException e) {
				continue;
			}
			if((!timeTag.contains("second") && !timeTag.contains("minute") && 
					!timeTag.contains("hour")))
				removalList.add(j);
		}
		
		// move posts from postList to oldPosts
		for(int k =removalList.size()-1; k>=0; k--) {
			oldPosts.add(postList.get(removalList.get(k)));
			postList.remove(removalList.get(k));
		}
		removalList.clear();
		
		// Main loop, goes on forever, always watching, always moderating
		// Well, until you quit Java
		while(true){
			// Pause for a few minutes. This lets Voat's servers handle more
			// traffic. Time set in the global variables
			System.out.println("waiting for "+waitTime+" minutes...");
			try {
				TimeUnit.MINUTES.sleep(waitTime);
			} catch(InterruptedException ex) {
				// No one cares if its sleep gets interrupted, its just a bot
			}
			
			// Refresh, then check for new posts/changed posts
			System.out.println("refreshing page and checking posts...");
			driver.navigate().refresh();
			waitForElement(driver,"submission");
			List<WebElement> roughPosts = driver.findElements(By.className(
					"submission"));
			ArrayList<String> roughPostIds = new ArrayList<String>();
			for(int m=0; m<roughPosts.size(); m++) {
				roughPostIds.add(roughPosts.get(m).getAttribute(
						"data-fullname"));
			}
			
			// Create a list of new posts
			ArrayList<WebElement> newPosts = new ArrayList<WebElement>();
			for(int l = 0; l<roughPosts.size(); l++) {
				WebElement somePost = roughPosts.get(l);
				String onetext = somePost.getAttribute("data-fullname");
				boolean foundID = false;
				for(int m = 0; m<postList.size(); m++) {
					String compareText = postList.get(m).get(0);
					if(onetext.contains(compareText)) {
						foundID = true;
						break;
					}
				}
				if(!foundID) {
					for(int m = 0; m<oldPosts.size(); m++) {
						String compareText = oldPosts.get(m).get(0);
						if(onetext.contains(compareText)) {
							foundID = true;
							break;
						}
					}
				}
				if(!foundID) newPosts.add(somePost);
			}
			System.out.println("found "+newPosts.size()+" new posts!");
			ArrayList<String> newPostIds = new ArrayList<String>();
			for(int m=0; m<newPosts.size(); m++) {
				newPostIds.add(newPosts.get(m).getAttribute("data-fullname"));
			}
			
			// for each in newposts, go into post
			for(int m=newPosts.size()-1; m>=0; m--) {
				WebElement inspectPost = null;
				try {
					inspectPost = driver.findElement(By.className(
							"id-"+newPostIds.get(m)));
				} catch(org.openqa.selenium.StaleElementReferenceException e) {
					System.out.println("Next new post cannot be found! Your "
							+"computer may be too slow, or the post was "
							+"deleted already, or this subverse may be too"
							+ "busy! Continuing on...");
					continue;
				}
				System.out.println("inspecting post with ID: "
						+newPostIds.get(m)+"...");
				inspectPost.findElement(By.className("comments")).click();
				waitForElement(driver,"commenttextarea");
				boolean isDeleted = false;
				String title = driver.findElement(By.className("entry"))
						.findElement(By.className("may-blank"))
						.getText().toLowerCase();
				// check if it is in violation of link or text only
				if(postMode != PostType.BOTH) {
					String domain = driver.findElement(By.className("domain"))
							.getText();
					if(postMode == PostType.TEXT && !domain.contains("self."
							+subName)) {
						// add comment with reason for deletion and delete
						postComment(driver,
									"This submission is being deleted because "
									+"it is a link post, and this subverse is "
									+"text only. Please respect this "
									+"community's rules. \n \n Beep. Boop. I "
									+"am a bot. If you believe this was "
									+"removed by mistake, send a message to "
									+"the other mods with a direct link to "
									+"this post.",true);
						driver.findElement(By.className("del-button"))
							.findElement(By.tagName("a")).click();
						waitForElement(driver, "yes");
						driver.findElement(By.className("yes")).click();
						notTooFast(10);
						isDeleted = true;
					}
					if(postMode == PostType.LINK && domain.contains("self."
							+subName)) {
						// add comment with reason for deletion and delete
						postComment(driver,
								"This submission is being deleted because it"
								+" is a text post, and this subverse is link"
								+" only. Please respect this community's rules"
								+". \n \n Beep. Boop. I am a bot. If you"
								+" believe this was removed by mistake, "
								+"send a message to the other mods with a "
								+"direct link to this post.",true);
						driver.findElement(By.className("del-button"))
							.findElement(By.tagName("a")).click();
						waitForElement(driver, "yes");
						driver.findElement(By.className("yes")).click();
						notTooFast(10);
						isDeleted = true;
					}
				}
				// check to see if the link is to a banned domain
				String domain = driver.findElement(By.className("domain"))
                        .getText();
				for(int i=0; i<domainBan.length; i++) {
					if(domain.contains(domainBan[i])) {
					    //if you don't want to post a comment, delete from here
					    postComment(driver,
					        "This submission is being deleted because it"
					        +" links to a domain which the moderators "
					        +"have banned. Please respect this comm"
					        +"unity's rules. \n \n Beep. Boop. I am a bot"
					        +". If you believe this was removed by mistake, "
					        +"send a message to the other mods with a "
					        +"direct link to this post.",true);
					    //...to here.
					    if(!isDeleted) {
						    driver.findElement(By.className("del-button"))
						        .findElement(By.tagName("a")).click();
						    waitForElement(driver, "yes");
						    driver.findElement(By.className("yes")).click();
						    notTooFast(10);
						    isDeleted = true;
					    }
					}
				}
				// check to see if the link is to a banned subverse
				String postURL = driver.findElement(By.className("entry"))
			            .findElement(By.className("may-blank")).getAttribute(
			            		"href").toLowerCase();
				for(int i=0; i<subBan.length; i++) {
					if(postURL.contains("voat.co/v/"+subBan[i]+"/")) {
					    //if you don't want to post a comment, delete from here
					    postComment(driver,
					        "This submission is being deleted because it"
					        +" links to a sub which the moderators "
					        +"have banned. Please respect this comm"
					        +"unity's rules. \n \n Beep. Boop. I am a bot"
					        +". If you believe this was removed by mistake, "
					        +"send a message to the other mods with a "
					        +"direct link to this post.",true);
					    //...to here.
					    if(!isDeleted) {
						    driver.findElement(By.className("del-button"))
						        .findElement(By.tagName("a")).click();
						    waitForElement(driver, "yes");
						    driver.findElement(By.className("yes")).click();
						    notTooFast(10);
						    isDeleted = true;
					    }
					}
				}
				// check title and message for rule violations
				for(int n = 0; n<titleBan.length; n++) {
					String check = titleBan[n];
					if(title.contains(check)) {
						// add comment and delete
						postComment(driver,
								"This submission is being deleted because "
								+"the title contains words or phrases that are"
								+" not allowed under this subverse's rules."
								+" Please respect this community's rules."
								+" \n \n Beep. Boop. I am a bot. If you"
								+" believe this was removed by mistake, "
								+"send a message to the other mods with a "
								+"direct link to this post.",true);
						if(!isDeleted) {
							driver.findElement(By.className("del-button"))
								.findElement(By.tagName("a")).click();
							waitForElement(driver, "yes");
							driver.findElement(By.className("yes")).click();
							notTooFast(10);
							isDeleted = true;
						}
					}
					
				}
				List<WebElement> hasMessage = driver.findElements(By.className(
						"original"));
				String message = "";
				if(hasMessage.size()!=0) {
					message = hasMessage.get(0).getText().toLowerCase();
				}
				for(int o=0; o<messBan.length; o++) {
					String check = messBan[o];
					if(message.contains(check)) {
						// add comment and delete
						postComment(driver,
								"This submission is being deleted because "
								+"the message contains words or phrases that "
								+"are not allowed under this subverses rules."
								+" Please respect this community's rules."
								+" \n \n Beep. Boop. I am a bot. If you"
								+" believe this was removed by mistake, "
								+"send a message to the other mods with a "
								+"direct link to this post.",true);
						if(!isDeleted) {
							driver.findElement(By.className("del-button"))
								.findElement(By.tagName("a")).click();
							waitForElement(driver, "yes");
							driver.findElement(By.className("yes")).click();
							notTooFast(10);
							isDeleted = true;
						}
					}
				}
				// flair post
				if(!isDeleted) {
					for(int p=0; p<flairs.length; p++) {
						String search = flairs[p];
						if(title.contains("["+search+"]")) {
							tooManyButtons(driver,"togglebutton","flair");
							waitForElement(driver,"sr-only");
							tooManyButtons(driver,"btn-xs",flairList[p]);
						}
					}
				}
				notTooFast(5);
				moderateComments(driver, driver.findElement(By.className(
						"id-"+newPostIds.get(m))));
				// add to postList
				addToPostList(driver.findElement(By.className(
						"id-"+newPostIds.get(m))), postList);
				driver.get(mainPage);
				waitForElement(driver,"submission");
			}
			
			// for each in roughposts
			ArrayList<String> agedPostIds = 
					new ArrayList<String>();
			for(int q=0; q<roughPostIds.size(); q++) {
				// add to agedposts if older than 24 hr,
				for(int j = 0; j<postList.size(); j++ ) {
					String thisPostID = roughPostIds.get(q);
					String timeTag = "";
					try {
						timeTag = driver.findElement(By.className(
							"id-"+thisPostID)).findElement(By.tagName("time"))
							.getText().split(" ")[1];
					}
					catch(java.lang.ArrayIndexOutOfBoundsException e) {
						continue;
					}
							
					if(!timeTag.contains("second") && !timeTag.contains(
							"minute") && !timeTag.contains("hour"))
						agedPostIds.add(thisPostID);
				}
			}
			// check between agedposts and oldposts, add differences to
			// differenceIds
			ArrayList<String> differenceIds = new ArrayList<String>();
			for(int r=0; r<agedPostIds.size(); r++) {
				String postId = agedPostIds.get(r);
				boolean included = false;
				for(int s=0; s<oldPosts.size(); s++) {
					if(postId.contains(oldPosts.get(s).get(0))) {
						included = true;
						break;
					}
				}
				if(!included) differenceIds.add(agedPostIds.get(r));
			}
			
			// check comments on posts with more comments than before
			for(int u=roughPosts.size()-1; u>=0; u--) {
				for(int v=0; v<postList.size(); v++) {
					if(roughPostIds.get(u).contains(postList.get(v).get(0))) {
						int oldCom = new Integer(postList.get(v).get(1));
						int newCom = 0;
						String comText = driver.findElement(By.className(
								"id-"+roughPostIds.get(u))).findElement(
								By.className("comments")).getText()
								.split(" ")[0];
						if(!comText.contains("discuss")) newCom = new Integer(
								comText);
						if(newCom > oldCom) {
							driver.findElement(By.className("id-"+
									roughPostIds.get(u))).findElement(By.
											className("comments")).click();
							waitForElement(driver,"commenttextarea");
							moderateComments(driver, driver.findElement(By
									.className("id-"+roughPostIds.get(u))));
						}
						// set archived # comments
						postList.get(v).set(1, Integer.toString(newCom));
					}
				}
			}
			
			// move old posts from postlist to oldposts
			ArrayList<Integer> removeList = new ArrayList<Integer>();
			for(int x=0; x<differenceIds.size(); x++) {
				String postID = differenceIds.get(x);
				for(int y=0; y<postList.size(); y++) {
					if(postList.get(y).get(0) == postID) {
						removeList.add(y);
						break;
					}
				}
			}
			for(int z=removeList.size()-1; z>=0; z--) {
				oldPosts.add(postList.get(removeList.get(z)));
				postList.remove(z);
			}
			
			// delete entries in oldPosts when they're not shown anymore
			// same for posts no longer visible
			ArrayList<Integer> notherRemoval = new ArrayList<Integer>();
			ArrayList<Integer> postRemoval = new ArrayList<Integer>();
			for(int j=0; j<roughPostIds.size(); j++) {
				int foundOld = -1;
				int foundPost = -1;
				for(int i=0; i<oldPosts.size(); i++) {
					if(oldPosts.get(i).get(0).contains(roughPostIds.get(j))) {
						foundOld = i;
						break;
					}
				}
				for(int k=0; k<postList.size(); k++) {
					if(postList.get(k).get(0).contains(roughPostIds.get(j))) {
						foundPost = k;
						break;
					}
				}
				if(foundOld!=-1) notherRemoval.add(foundOld);
				if(foundPost!=-1) postRemoval.add(foundPost);
			}
			for(int k=notherRemoval.size()-1; k>=0; k--) {
				oldPosts.remove(notherRemoval.get(k));
			}
			for(int k=postRemoval.size()-1; k>=0; k--) {
				postList.remove(postRemoval.get(k));
			}
			
			// get the date and compare it to the special times to post
			Calendar cal = Calendar.getInstance();
			Date date = cal.getTime();
			String dateStr = dateFormat.format(date);
			for(int d=0; d<specDates.length; d++) {
				if(dateStr.contains(specDates[d])) {
					/* TODO: Structure for predefined posts, uncomment, change
					 * messages and title if using. Copy/paste this and change
					 * the first if statement for more than one premade post.
					 * This also assumes your account has enough ccp to avoid
					 * reCaptcha, otherwise it'll get stuck!
					if(d == 0 && !dated1) {
						dated1 = true;
						postText(driver,"Test post, please ignore",
								"Allan, please add details",false);
						// flair the post, leave commented if no flairs used
						tooManyButtons(driver,"main","flair");
						waitForElement(driver,"btn-xs");
						tooManyButtons(driver,"btn-xs","Referendum");
						driver.get(mainPage);
					}
					 *
					 * end premade post template
					 */
				}
			}
			// reset the date triggers
			for(int h=0; h<dateResets.length; h++) {
				if(dateStr.contains(dateResets[h])) {
					if(h == 0 && dated1) dated1 = false;
					//if(h == 1 && dated2) dated2 = false;
					//etc...
				}
			}
			driver.get(mainPage);
			waitForElement(driver,"submission");

			// clean up lists to fix memory leaking (kinda)
			roughPostIds.clear();
			roughPosts.clear();
			notherRemoval.clear();
			removeList.clear();
			postRemoval.clear();
			newPosts.clear();
			agedPostIds.clear();
		}

	}
	
	/**
	 * Function to add posts to the main post list
	 * @param aPost The post to add to the list
	 * @param postList The post to add the post to
	 */
	public static void addToPostList(WebElement aPost, 
			ArrayList<ArrayList<String>> postList) {
		String postID = aPost.getAttribute("data-fullname");
		String commentText = aPost.findElement(By.className(
				"comments")).getText();
		String numComments = "0";
		if(commentText.contains(" "))numComments=commentText.split(" ")[0];
		// The fact that lists/arraylists can't be made and initialized
		// in one line is stupid. Just sayin'
		ArrayList<String> newData = new ArrayList<String>();
		newData.add(postID);
		newData.add(numComments);
		postList.add(newData);
	}
	
	/**
	 * Function to wait until a specified element appears, use to wait out
	 * Voat's DDoS protection and sit through the animations and such
	 * @param driver The current WebDriver in use
	 * @param element The element to wait to be displayed
	 */
	public static void waitForElement(WebDriver driver, String element) {
		while(true) {
			List<WebElement> loadedDiv = driver.findElements(By.className(
					element));
			if(loadedDiv.size() > 0 ) {
				boolean canbreak = false;
				for(int f=0; f<loadedDiv.size(); f++) {
					if(loadedDiv.get(f).isDisplayed()) {
						canbreak = true;
						break;
					}
				}
				if(canbreak) break;
			}
		}
		// wait an extra 10 seconds, Voat thinks automod is too fast
		notTooFast(10);
	}
	
	/**
	 * Function to wait a specified amount of time
	 * @param n the number of seconds to pause
	 */
	public static void notTooFast(int n) {
		try {
			TimeUnit.SECONDS.sleep(n);
		} catch(InterruptedException ex) {
			// No one cares if its sleep gets interrupted, its a bot
		}
	}
	
	/**
	 * Function to click one button in a group that is inseparable
	 * @param driver The WebDriver in use
	 * @param className The class with multiple button elements
	 * @param buttonName The label on the button to press
	 */
	public static void tooManyButtons(WebDriver driver, String className, 
			String buttonName) {
		List<WebElement> buttons = driver.findElements(
				By.className(className));
		for(int q=0; q<buttons.size(); q++) {
			String buttName = buttons.get(q).getText();
			if(buttName.contains(buttonName)) {
				buttons.get(q).click();
				break;
			}
		}
	}
	
	/**
	 * Function to moderate the comments to avoid copy/pasta code
	 * @param driver The webdriver in use
	 * @param post The post to check comments in
	 */
	public static void moderateComments(WebDriver driver, WebElement post) {
		// load ALL the comments
		while(driver.findElements(By.id("loadmorebutton")).size() != 0) {
			driver.findElement(By.id("loadmorebutton")).click();
			notTooFast(5);
		}
		List<WebElement> comments=driver.findElements(By.className("comment"));
		ArrayList<WebElement> newComments = new ArrayList<WebElement>();
		for(int a=0; a<comments.size(); a++) {
			List<WebElement> times = comments.get(a).findElements(By
					.tagName("time"));
			String timestamp = "";
			for(int i=0; i<times.size(); i++) {
				if(times.get(i).isDisplayed()) timestamp=times.get(i)
						.getText();
			}
			if(timestamp=="") continue;
			int timething = new Integer(timestamp.replaceAll("[^0-9]", ""));
			if((timestamp.contains("second") || timestamp.contains("minute"))
					&& timething<=waitTime) {
				newComments.add(comments.get(a));
			}
		}
		System.out.println("found "+newComments.size()+
				" new comments in this post!");
		for(int b=0; b<newComments.size(); b++) {
			String comText = newComments.get(b).findElement(By.className(
					"entry")).findElement(By.className("md")).getText()
					.toLowerCase();
			boolean beenDelete = false;
			List<WebElement> commLinks = newComments.get(b).findElements(
					By.tagName("a"));
			for(int j=0; j<commLinks.size(); j++) {
			    for(int i=0; i<domainBan.length; i++) {
			    	String checAtt = commLinks.get(j).getAttribute("href");
			    	if(checAtt==null) break;
			        if(checAtt.toLowerCase()
			        		.contains(domainBan[i])) {
			            // if you don't want a comment to be posted, delete 
			        	// from here...
			            commentReply(driver,newComments.get(b),"Your comment "
			            		+"has been deleted because it contains links "
			                    +"to domains that have been banned by "
			                    +"the moderators. Please respect "
			                    +"this community's rules."
			                    +" \n \n Beep. Boop. I am a bot. If you"
			                    +" believe this was removed by mistake, "
			                    +"send a message to the other mods with a "
			                    +"direct link to this post.",true);
			            //...to here.
			            if(!beenDelete) {
			                newComments.get(b).findElement(By.className(
			                        "del-button")).findElement(By.tagName("a"))
			                        .click();
			                waitForElement(driver, "yes");
			                newComments.get(b).findElement(By.className("yes"))
			                        .click();
			                notTooFast(10);
			            }
			            beenDelete = true;
			            break;
			        }
			    }
			    if(beenDelete) break;
			    for(int k=0; k<subBan.length; k++) {
			    	String checAtt = commLinks.get(j).getAttribute("href");
			    	if(checAtt==null) break;
			        if(checAtt.toLowerCase().contains("voat.co/v/"
			        		+subBan[k]+"/")) {
			            // if you don't want a comment to be posted, delete 
			        	// from here...
			            commentReply(driver,newComments.get(b),"Your comment "
			            		+"has been deleted because it contains links "
			                    +"to a subs have been banned by "
			                    +"the moderators. Please respect "
			                    +"this community's rules."
			                    +" \n \n Beep. Boop. I am a bot. If you"
			                    +" believe this was removed by mistake, "
			                    +"send a message to the other mods with a "
			                    +"direct link to this post.",true);
			            //...to here.
			            if(!beenDelete) {
			                newComments.get(b).findElement(By.className(
			                        "del-button")).findElement(By.tagName("a"))
			                        .click();
			                waitForElement(driver, "yes");
			                newComments.get(b).findElement(By.className("yes"))
			                        .click();
			                notTooFast(10);
			            }
			            beenDelete = true;
			            break;
			    	}
			    }
			    if(beenDelete) break;
			}
			if(!beenDelete) {
				for(int c=0; c<commBan.length; c++) {
					if(comText.contains(commBan[c])) {
						commentReply(driver,newComments.get(b),"Your comment "
								+"has been deleted because it contains the "
								+"word '"+commBan[c]+".' Please respect "
								+"this community's rules."
								+" \n \n Beep. Boop. I am a bot. If you"
								+" believe this was removed by mistake, "
								+"send a message to the other mods with a "
								+"direct link to this post.",true);
						if(!beenDelete) {
							newComments.get(b).findElement(By.className(
									"del-button")).findElement(By.tagName("a"))
									.click();
							waitForElement(driver, "yes");
							newComments.get(b).findElement(By.className("yes"))
								.click();
							notTooFast(10);
						}
						beenDelete = true;
						break;
					}
				}
			}
		}
		driver.get(mainPage);
		waitForElement(driver,"submission");
	}
	
	/**
	 * Creates a text post with a given message and title
	 * @param driver The current WebDriver
	 * @param title The Title for the post
	 * @param message The Message for the post
	 * @param sticky Whether to sticky this post, default is false
	 */
	public static void postText(WebDriver driver,String title,String message) {
		postText(driver,title,message,false);
	}
	public static void postText(WebDriver driver,String title,String message,
			boolean sticky) {
		driver.findElement(By.className("submit-text")).click();
		waitForElement(driver,"form-control");
		// input post title
		driver.findElement(By.id("Title")).sendKeys(title);
		// put in post message
		driver.findElement(By.id("Content")).sendKeys(message);
		notTooFast(5);
		List<WebElement> findTheButton = driver.findElements(
			By.className("btn-whoaverse"));
		for(int e=0; e<findTheButton.size(); e++) {
			String typeThing = findTheButton.get(e).getAttribute("type");
			if(typeThing == "submit") {
				findTheButton.get(e).click();
				break;
			}
		}
		waitForElement(driver,"commenttextarea");
		// sticky
		if(sticky) {
			List<WebElement> findDist = driver.findElements(By.className(
					"togglebutton"));
			for(int i=0; i<findDist.size(); i++) {
				if(findDist.get(i).getText().contains("toggle sticky")) {
					findDist.get(i).click();
					break;
				}
			}
		}
	}
	
	/**
	 * Posts a link with specified title and URL
	 * @param driver The current WebDriver
	 * @param title The title for the post
	 * @param url The URL for the link
	 * @param sticky Whether to sticky this post, default is false
	 */
	public static void postLink(WebDriver driver,String title,String url) {
		postLink(driver,title,url,false);
	}
	public static void postLink(WebDriver driver,String title,String url,
			boolean sticky) {
		driver.findElement(By.className("submit-text")).click();
		waitForElement(driver,"form-control");
		// input post title
		driver.findElement(By.id("LinkDescription")).sendKeys(title);
		// put in post message
		driver.findElement(By.id("Content")).sendKeys(url);
		notTooFast(5);
		List<WebElement> findTheButton = driver.findElements(
			By.className("btn-whoaverse"));
		for(int e=0; e<findTheButton.size(); e++) {
			String typeThing = findTheButton.get(e).getAttribute("type");
			if(typeThing == "submit") {
				findTheButton.get(e).click();
				break;
			}
		}
		waitForElement(driver,"commenttextarea");
		// sticky
		if(sticky) {
			List<WebElement> findDist = driver.findElements(By.className(
					"togglebutton"));
			for(int i=0; i<findDist.size(); i++) {
				if(findDist.get(i).getText().contains("toggle sticky")) {
					findDist.get(i).click();
					break;
				}
			}
		}
	}
	
	/**
	 * Function to post a top-level comment to a post already loaded up
	 * @param driver Current webdriver in use
	 * @param message The message to write
	 * @param distinguish Whether to distinguish this comment, default is false
	 */
	public static void postComment(WebDriver driver, String message) {
		postComment(driver, message, false);
	}
	public static void postComment(WebDriver driver,String message,boolean 
			distinguish) {
		List<WebElement> findComments = driver.findElements(By.className(
				"commenttextarea"));
		for(int f=0; f<findComments.size(); f++) {
			if(findComments.get(f).isDisplayed()) {
				findComments.get(f).sendKeys(message);
				notTooFast(5);
				driver.findElement(By.id("submitbutton")).click();
				break;
			}
		}
		notTooFast(10);
		// distinguish
		if(distinguish) {
			List<WebElement> findDist = driver.findElements(By.tagName("a"));
			for(int i=0; i<findDist.size(); i++) {
				if(findDist.get(i).getText().contains("distinguish")) {
					findDist.get(i).click();
					break;
				}
			}
		}
	}
	
	/**
	 * Function to reply to a comment
	 * @param driver The current WebDriver
	 * @param element The post to reply to
	 * @param message The message to put in the comment
	 * @param distinguish Whether to distinguish the comment, default is false
	 */
	public static void commentReply(WebDriver driver,WebElement element,String
			message) {
		commentReply(driver,element,message,false);
	}
	public static void commentReply(WebDriver driver,WebElement element,String 
			message,boolean distinguish) {
		List<WebElement> buttons = element.findElement(By.className("buttons"))
				.findElements(By.tagName("a"));
		for(int q=0; q<buttons.size(); q++) {
			String buttName = buttons.get(q).getText();
			if(buttName.contains("reply") && buttons.get(q).isDisplayed()) {
				buttons.get(q).click();
				break;
			}
		}
		waitForElement(driver, "pmreplyform");
		List<WebElement> findComments = element
				.findElements(By.className("commenttextarea"));
		for(int f=0; f<findComments.size(); f++) {
			if(findComments.get(f).isDisplayed()) {
				findComments.get(f).sendKeys(message);
				notTooFast(5);
				element.findElement(By.id("submitbutton")).click();
				break;
			}
		}
		notTooFast(10);
		// distinguish
		if(distinguish) {
			List<WebElement> findDist = driver.findElements(By.tagName("a"));
			for(int i=0; i<findDist.size(); i++) {
				if(findDist.get(i).getText().contains("distinguish")) {
					findDist.get(i).click();
					break;
				}
			}
		}
	}
	
	/**
	 * Function to send a message to another user, can be used for modmail, 
	 * daily reports or whatever. Also requires the account in use has enough 
	 * CCP to send a message so reCaptcha doesn't show up
	 * @param driver The current WebDriver
	 * @param user The username to send to
	 * @param subject The subject to enter for the message
	 * @param message The message to send
	 */
	public static void sendMessage(WebDriver driver,String user,String subject,
			String message) {
		driver.get("http://voat.co/messaging/compose");
		waitForElement(driver,"form-control");
		driver.findElement(By.id("Recipient")).sendKeys(user);
		driver.findElement(By.id("Subject")).sendKeys(subject);
		driver.findElement(By.id("Body")).sendKeys(message);
		notTooFast(10);
		List<WebElement> findSend = driver.findElements(By.tagName("input"));
		for(int i=0; i<findSend.size(); i++) {
			if(findSend.get(i).getAttribute("value").contains("Send")) {
				findSend.get(i).click();
				break;
			}
		}
		notTooFast(10);
		driver.get(mainPage);
		waitForElement(driver,"submission");
	}

}
