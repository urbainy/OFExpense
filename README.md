# OFExpense

`OF` stands for `Oh Family` which is a series software focus on family scope, including children
education and couple's finance.

`Expense` means family cost. I made this application to record family member's expense and calculate
the sharing and balance.

This application use P2P architecture to work, no server needed.

You can define your expense class and sharing percentage and then record expenses according to those
classes
individually. You also can use this application by yourself. In this case, you just leave the
sharing percentage as 100%.

After you have several records of expenses, you can go to `Statistics` screen to see the summaries.
If you want to see both parties' statistics, you need to synchronize data firstly.

Go to the synchronize screen to do this job. Choose one party to be
the server and the other to be the client. Don't choose server and client on the same phone. It
could damage the data.

Now only two parties(members) are supported, i.e. wife and husband. No more member can be supported
until
now. Pay attention to this, or data could be a mass.

After synchronization, you will find some differently displayed records in the `Expenses` screen.
Those records without a `pen` icon means the other party created.

This application also allows you to `dump` or `load`(must have loadable data after dump operation)
all records from or to the database. This function makes two senses.

1. Save and reuse your history records data when you change or reset your phone.
2. Check the recorded data by your favorite data sheet software, like `Excel`, in case the Room
   database (SQLite) on the phone can not fulfill your requirement.

Go to the public `Documents` folder of your phone's file manager, find `category.csv`, `expense.csv`
and `preference.csv` the three files to accomplish this function.

Enjoy it!

Not well tested. Propose to run on Qualcomm Snapdragon platform. According to my testing, MediaTek
Dimensity platform can only work as client.
