package com.rs.game.content.quests.tribaltotem;

import com.rs.engine.book.Book;
import com.rs.engine.book.BookPage;

public class RealEstateGuideBook extends Book {

	public RealEstateGuideBook() {
		super("Ardougne Estate Guide",
				new BookPage(
						new String[] {
								"<col=FF0000>Introduction</col>",
								"This book is your",
								"city of Ardougne.",
								"Ardougne is known as an",
								"exciting modern city",
								"located on the sun drenched",
								"southern coast of Kandarin.",
								"It seems to like this",
								"combination a lot!",
								"",
								"",
						},
						new String[] {

								"<col=FF0000>Ardougne: City of Shopping!</col>",
								"Come sample the delights",
								"of the Ardougne market - the",
								"biggest in the known world!",
								"From spices to silk, there",
								"is something here for",
								"everybody! Other popular",
								"shopping destinations in",
								"the area include the Armoury",
								"and the ever popular",
								"Adventurers' supply store.",
						}),
				new BookPage(
						new String[] {
								"<col=FF0000>Ardougne: City of Fun!</col>",
								"If you're looking for",
								"entertainment in Ardougne,",
								"why not pay a visit to",
								"the Ardougne City Zoo?",
								"Or relax with a drink",
								"in the ever popular",
								"Flying Horse Inn?",
								"",
								"And for the adventurous,",
								"there are always rats",
						},

						new String[] {
								"to be slaughtered in the",
								"expansive and vermin-ridden ",
								"sewers. There is truly",
								"something for everybody here! ",
								"",
								"",
								"",
								"",
								"",
								"",
								"",
						}),
				new BookPage(
						new String[] {
								"<col=FF0000>Ardougne: City of History!</col>",
								"Ardougne is renowned as",
								"an important city of",
								"historical interest. One",
								"historic building is the",
								"magnificent Handelmort",
								"Mansion, currently owned by ",
								"Lord Francis Kurt Handelmort.",
								"Also of historical interest",
								"is Ardougne Castle in the ",
								"East side of the city,",
						},
						new String[] {
								"recently opened to the",
								"public. And of course,",
								"the Holy Order of Paladins",
								"still wander the streets",
								"of Ardougne, and are often",
								"of interest to tourists.",
								"",
								"<col=FF0000>Further Fields</col>",
								"Ardougne is also of great",
								"interest to the cultural",
								"tourist.",
						}),
				new BookPage(
						new String[] {
								"If you want to go further",
								"afield, why not have a",
								"look at the ominous",
								"Pillars of Zanash, the",
								"mysterious marble pillars",
								"located just West of",
								"the city? Or perhaps",
								"the town of Brimhaven, on",
								"the exotic island of",
								"SKaramja? It's only a",
								"short boat trip, with",
						},
						new String[] {
								"regular transport leaving",
								"from Ardougne Harbour at",
								"all times of the day,",
								"all year round.",
								"",
								"",
								"",
								"",
								"",
								"",
								"",
						})
				);
	}

}
