# VoatAutoMod
AutoModerator for Voat, created by @mikex5, uses the Selenium Library for browser automation (http://www.seleniumhq.org). This application/program/script thing comes with absolutely no warranty or license, you may modify and distribute this as you see fit.

This was created to be a tool to help lighten the workload on moderators and provide an automated way to do basic tasks. It is NOT meant to be a tool for trolls or spammers to shitpost everywhere. 

For technical support, message @mikex5 on Voat, he may or may not answer you based on how busy or sick of questions he is.

CHANGES:
--------
0.2.2 - Fixed numerous crashes and hang-ups, now properly supports deleting links to banned sites and subverses.

0.2 - Added support to delete submissions and comments that link to certain domains or subverses, fixed possible issues of old posts being removed prematurely and posts never being removed from postList if they're no longer visable, possible fix for stale references when moderating larger subverses.


HOW TO USE:
-----------

Because GitHub apparently hates me, download this .zip file, unzip it, and import it into Eclipse or whatever IDE you use. Modify the global variables (and any other code) as you see fit, then either run the Java application, or export it as an excecutable Jar and run it.


DETAILS:
--------

Most all of the basic functions are listed below, they can be modified by changing the global variables, also detailed below.

BASIC FUNCTIONS:
-Automatically tag posts
-Filter submissions and comments for 'badwords'
-Automatically post submissions at certain times in a week/day/year (requires sufficient ccp to disable reCaptcha, and some uncommenting way down in the code)
-Post comments to rule violaters stating why their posts/comments are being removed

GLOBAL VARIABLES (and what they do):
*subName - The name of the subverse to moderate, this is used for checking the domain (if a post is text), so lettercase matters!

*mainPage - The main page of the subverse to moderate, this can be left unchanged since it automatically goes to the new page of the subverse with subName as the name.

*userName - The username for the automoderator to use, recommend setting up an alternate account for this.

*password - The password to the account with the above username.

*waitTime - How much time to wait before refreshing the page and running moderator functions again. Busier subs will want to set this lower.

*titleBan - List of strings which if found in the title of a post will cause AutoMod to delete the post.

*messBan - List of strings which if found in the message of the post will cause AutoMod to delete the post.

*commBan - List of strings which if found in a comment will cause AutoMod to delete a comment.

*domainBan - List of domain names which will cause a comment or post to be removed if they're linked to

*subBan - List of subverse names that will cause a comment or post to be removed if they're linked to

*postMode - Enum specifying what mode the subverse is in. Any post that is not of the type set here will be deleted.

*flairs - List of flairs that the users use. These must be strings of the lowercase version of what users would put inside square brackets.

*flairList - List of the corresponding flair names to the list of flairs above. These are the flair names, case matters and this list must be the same size as flairs.

*specDates - List of special dates and times to post. If you want to post on a certain day of the week at a certain hour, use "HH/DDDDDDDD", if you want to post on a certain day of the year, use "MM/DD". For each entry in here, a boolean must also be added to tell when the post has been made, and an entry by the TODO in main() must be added. A premade post example is commented out in there as an example.

*dateResets - When to reset the boolean related to the specPost dates. Uses the same date format and this list must be the same length as specDates.


ADDITIONAL STUFF
----------------

Not much else to say, have fun using it and I'll try and provide technical support when I can.
